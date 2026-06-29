package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.domain.RequirementHistory;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.infrastructure.RequirementHistoryRepository;
import requirementsAssistantAI.infrastructure.RequirementRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;
import requirementsAssistantAI.infrastructure.EmbeddingSimilarityRepository;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.dto.RequirementDTO;
import requirementsAssistantAI.dto.RequirementHistoryDTO;
import requirementsAssistantAI.dto.SaveRequirementRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import requirementsAssistantAI.dto.RequirementReportDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.langchain4j.data.document.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RequirementService {

    private static final Logger log = LoggerFactory.getLogger(RequirementService.class);

    @Value("${ai.context.max-approved-results:5}")
    private int maxApprovedResults;
    @Value("${ai.context.max-length:2500}")
    private int maxContextLength;
    @Value("${ai.report.intention-filter.enabled:false}")
    private boolean intentionFilterEnabled;

    private final AssistantAiService assistantAiService;
    private final RequirementRepository requirementRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final RequirementHistoryRepository requirementHistoryRepository;
    private final RagRetrievalService ragRetrievalService;
    private final ApplicationEventPublisher eventPublisher;
    private final EmbeddingSimilarityRepository similarityRepository;

    @Value("${ai.report.max-similar-pairs:300}")
    private int maxSimilarPairs;

    public RequirementService(
            AssistantAiService assistantAiService,
            RequirementRepository requirementRepository,
            RequirementSetRepository requirementSetRepository,
            RequirementHistoryRepository requirementHistoryRepository,
            RagRetrievalService ragRetrievalService,
            ApplicationEventPublisher eventPublisher,
            EmbeddingSimilarityRepository similarityRepository) {
        this.assistantAiService = assistantAiService;
        this.requirementRepository = requirementRepository;
        this.requirementSetRepository = requirementSetRepository;
        this.requirementHistoryRepository = requirementHistoryRepository;
        this.ragRetrievalService = ragRetrievalService;
        this.eventPublisher = eventPublisher;
        this.similarityRepository = similarityRepository;
    }

    /**
     * Refina o requisito com IA sem persistir no banco.
     * Usuário pode editar prompt e requisito refinado antes de salvar.
     */
    @Transactional(readOnly = true)
    public RequirementDTO refineRequirement(String rawRequirement, @NonNull UUID requirementSetId) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(requirementSetId))
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto) não encontrado com ID: " + requirementSetId));

        AiRefinementResult result = processWithAI(rawRequirement, requirementSetId.toString(), requirementSet.getName(), requirementSet.getDescription());

        LocalDateTime now = LocalDateTime.now();
        return new RequirementDTO(
                null,
                "REQ-TEMP",
                rawRequirement,
                result.refinedRequirementText(),
                result.analise(),
                result.ambiguityWarnings(),
                now,
                now,
                requirementSet.getId(),
                requirementSet.getName()
        );
    }

    private record AiRefinementResult(String analise, String refinedRequirementText, List<String> ambiguityWarnings) {}

    private AiRefinementResult processWithAI(String rawRequirement, String requirementSetId, String requirementSetName, String requirementSetDescription) {
        String relevantContext = findHybridContext(
                rawRequirement, requirementSetId, requirementSetName, requirementSetDescription);

        String aiResponse = null;
        int retryCount = 0;
        final int MAX_RETRIES = 2;
        final int MIN_RESPONSE_LENGTH = 200;

        while (retryCount <= MAX_RETRIES) {
            try {
                aiResponse = assistantAiService.refineRequirement(rawRequirement, relevantContext);

                if (aiResponse != null && aiResponse.length() >= MIN_RESPONSE_LENGTH) {
                    break;
                } else if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    Thread.sleep(1000);
                    continue;
                } else {
                    break;
                }
            } catch (Exception e) {
                log.warn("Erro ao chamar IA (tentativa {}/{}): {} - {}", retryCount + 1, MAX_RETRIES + 1, e.getClass().getSimpleName(), e.getMessage());
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                log.error("IA falhou após todas as tentativas. Verifique GEMINI_API_KEY e logs do Cloud Run.", e);
                aiResponse = null;
                break;
            }
        }

        boolean isAiResponseValid = aiResponse != null && aiResponse.length() >= 200;
        boolean isPartialProcessing = aiResponse == null || !isAiResponseValid;

        String analise = isPartialProcessing ? null : extractAnalise(aiResponse);
        String refinedRequirementText = isPartialProcessing ? null : extractRefinedRequirement(aiResponse);
        List<String> ambiguityWarnings = isPartialProcessing ? List.of() : extractAmbiguityWarnings(aiResponse);

        if (refinedRequirementText == null || refinedRequirementText.trim().isEmpty() || refinedRequirementText.length() < 20) {
            if (isAiResponseValid) {
                refinedRequirementText = aiResponse;
            } else {
                refinedRequirementText = "Como usuário do sistema, eu quero " + rawRequirement.toLowerCase() + " para melhorar a gestão do sistema.";
            }
        }

        if (analise == null || analise.trim().isEmpty() || analise.length() < 20 || "Verifique o texto completo.".equals(analise)) {
            if (isAiResponseValid) {
                analise = aiResponse;
            } else {
                String partialWarning = isPartialProcessing ? "⚠️ PROCESSAMENTO PARCIAL: A IA não respondeu corretamente. " : "";
                analise = partialWarning + "O requisito foi processado, mas a resposta da IA foi incompleta. Requisito original: " + rawRequirement;
            }
        }

        List<String> sanitizedWarnings = new ArrayList<>();
        for (String w : ambiguityWarnings != null ? ambiguityWarnings : List.<String>of()) {
            String sanitized = sanitizePlainText(w);
            if (sanitized != null && !sanitized.isBlank()) {
                sanitizedWarnings.add(sanitized);
            }
        }
        return new AiRefinementResult(
                sanitizePlainText(analise),
                sanitizePlainText(refinedRequirementText),
                sanitizedWarnings
        );
    }

    private String sanitizePlainText(String text) {
        if (text == null || text.isBlank()) return text;
        return text
                .replaceAll("\\*+", "")
                .replaceAll("#+", "")
                .replaceAll("\\s{2,}", " ")
                .replaceFirst("^\\s*:?\\s*", "")
                .trim();
    }


    @Transactional
    public RequirementDTO saveRequirement(@NonNull SaveRequirementRequest request) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(request.getRequirementSetId()))
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto) não encontrado com ID: " + request.getRequirementSetId()));

        String requirementId = generateNextRequirementId(request.getRequirementSetId());
        String finalText = request.isUseRefinedVersion() ? request.getRefinedRequirement() : request.getRawRequirement();

        String analise = request.getAnalise();
        List<String> ambiguityWarnings = request.getAmbiguityWarnings();
        if ((analise == null || analise.isBlank()) || (ambiguityWarnings == null || ambiguityWarnings.isEmpty())) {
            AiRefinementResult analysis = processWithAI(request.getRawRequirement(), request.getRequirementSetId().toString(), requirementSet.getName(), requirementSet.getDescription());
            if (analise == null || analise.isBlank()) analise = analysis.analise();
            if (ambiguityWarnings == null || ambiguityWarnings.isEmpty()) ambiguityWarnings = analysis.ambiguityWarnings();
        }

        Requirement requirement = new Requirement();
        requirement.setRequirementId(requirementId);
        requirement.setRawRequirement(request.getRawRequirement());
        requirement.setRefinedRequirement(finalText);
        requirement.setAnalise(analise);
        requirement.setAmbiguityWarnings(ambiguityWarnings);
        requirement.setRequirementSet(requirementSet);

        requirement = requirementRepository.save(requirement);

        requirementHistoryRepository.save(new RequirementHistory(requirement, "CREATED"));

        eventPublisher.publishEvent(new RequirementEmbeddingEvent(
                requirement.getUuid(), RequirementEmbeddingEvent.Operation.UPSERT));

        return convertToDTO(requirement);
    }

    @Transactional(readOnly = true)
    public RequirementDTO getRequirementById(@NonNull UUID id) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", id));
        return convertToDTO(requirement);
    }

    @Transactional(readOnly = true)
    public List<RequirementDTO> getRequirementsBySetId(@NonNull UUID requirementSetId) {
        List<Requirement> requirements = requirementRepository.findByRequirementSet_Id(Objects.requireNonNull(requirementSetId));
        return requirements.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RequirementDTO> getAllRequirements(UUID requirementSetId) {
        List<Requirement> requirements;
        if (requirementSetId != null) {
            requirements = requirementRepository.findByRequirementSet_Id(requirementSetId);
        } else {
            requirements = requirementRepository.findAll();
        }
        return requirements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RequirementHistoryDTO> getRequirementHistory(@NonNull UUID requirementId) {
        List<RequirementHistory> history = requirementHistoryRepository.findByRequirement_UuidOrderByCreatedAtDesc(Objects.requireNonNull(requirementId));
        return history.stream().map(this::convertToHistoryDTO).collect(Collectors.toList());
    }

    private RequirementHistoryDTO convertToHistoryDTO(RequirementHistory h) {
        return new RequirementHistoryDTO(
                h.getId(),
                h.getRequirement() != null ? h.getRequirement().getUuid() : null,
                h.getRequirementId(),
                h.getRawRequirement(),
                h.getRefinedRequirement(),
                h.getAnalise(),
                h.getAmbiguityWarnings(),
                h.getActionType(),
                h.getCreatedAt()
        );
    }

    @Transactional
    public RequirementDTO updateRequirement(@NonNull UUID requirementUuid, String rawRequirement, String refinedRequirement, boolean useRefinedVersion) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(requirementUuid))
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", requirementUuid));

        requirement.setRawRequirement(rawRequirement);
        String finalText = useRefinedVersion ? refinedRequirement : rawRequirement;
        requirement.setRefinedRequirement(finalText);

        requirement = requirementRepository.save(requirement);
        requirementHistoryRepository.save(new RequirementHistory(requirement, "UPDATED"));

        // Re-adiciona ao embedding store com texto atualizado (requisitos antigos podem ter versão desatualizada)
        eventPublisher.publishEvent(new RequirementEmbeddingEvent(
                requirement.getUuid(), RequirementEmbeddingEvent.Operation.UPSERT));

        return convertToDTO(requirement);
    }

    @Transactional
    public void deleteRequirement(@NonNull UUID requirementUuid) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(requirementUuid))
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", requirementUuid));

        requirementHistoryRepository.deleteAll(
            requirementHistoryRepository.findByRequirement_UuidOrderByCreatedAtDesc(requirementUuid)
        );

        requirementRepository.delete(requirement);
        eventPublisher.publishEvent(new RequirementEmbeddingEvent(
                requirementUuid, RequirementEmbeddingEvent.Operation.DELETE));
    }

    private RequirementDTO convertToDTO(Requirement requirement) {
        RequirementSet requirementSet = requirement.getRequirementSet();
        return new RequirementDTO(
                requirement.getUuid(),
                requirement.getRequirementId(),
                requirement.getRawRequirement(),
                requirement.getRefinedRequirement(),
                requirement.getAnalise(),
                requirement.getAmbiguityWarnings(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt(),
                requirementSet != null ? requirementSet.getId() : null,
                requirementSet != null ? requirementSet.getName() : null
        );
    }

    private String findHybridContext(
            String userQuery,
            String projectId,
            String requirementSetName,
            String requirementSetDescription) {
        StringBuilder context = new StringBuilder();
        if (requirementSetName != null && !requirementSetName.isBlank()) {
            context.append("PROJETO/CONJUNTO DE REQUISITOS: ").append(requirementSetName).append('\n');
        }
        if (requirementSetDescription != null && !requirementSetDescription.isBlank()) {
            context.append("DESCRIÇÃO DO PROJETO: ").append(requirementSetDescription).append("\n\n");
        }
        String retrieved = ragRetrievalService.retrieve(
                userQuery, UUID.fromString(projectId), maxApprovedResults, 0.7, maxContextLength);
        if (retrieved.isBlank()) {
            return context.append("Nenhum requisito anterior relevante encontrado neste projeto.").toString();
        }
        int remaining = Math.max(0, maxContextLength - context.length());
        context.append("REQUISITOS SALVOS NESTE PROJETO:\n");
        remaining = Math.max(0, maxContextLength - context.length());
        context.append(retrieved, 0, Math.min(retrieved.length(), remaining));
        return context.toString();
    }

    private String generateNextRequirementId(@NonNull UUID requirementSetId) {
        int maxNum = requirementRepository
                .findMaxRequirementNumber(Objects.requireNonNull(requirementSetId))
                .orElse(0);
        return String.format("REQ-%03d", maxNum + 1);
    }

    private String extractAnalise(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty() || aiResponse.length() < 50) {
            return null;
        }
        Pattern pattern1 = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Análise\\s*:?\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*(?:Requisito Refinado|Pontos de Ambiguidade|Status|Estimativa)|$)",
                Pattern.DOTALL);
        Matcher matcher1 = pattern1.matcher(aiResponse);
        if (matcher1.find()) {
            String result = matcher1.group(1).trim();
            if (!result.isEmpty() && result.length() >= 30 && !result.matches("^\\d+$")) {
                return result;
            }
        }
        int index = aiResponse.toLowerCase().indexOf("análise");
        if (index >= 0) {
            String after = aiResponse.substring(index + 7).trim();
            int idxRefinado = after.toLowerCase().indexOf("requisito refinado");
            int idxPontos = after.toLowerCase().indexOf("pontos de ambiguidade");
            int idxEstimativa = after.toLowerCase().indexOf("estimativa");
            int next = Integer.MAX_VALUE;
            if (idxRefinado >= 0) next = Math.min(next, idxRefinado);
            if (idxPontos >= 0) next = Math.min(next, idxPontos);
            if (idxEstimativa >= 0) next = Math.min(next, idxEstimativa);
            if (next > 0 && next < Integer.MAX_VALUE) {
                String result = after.substring(0, next).replaceFirst("^\\s*:?\\s*", "").trim();
                if (result.length() >= 30 && !result.matches("^\\d+$")) {
                    return result;
                }
            }
        }
        return null;
    }

    private String extractRefinedRequirement(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty() || aiResponse.length() < 50) {
            return null;
        }
        Pattern pattern1 = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Requisito Refinado\\s*:?\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*(?:Pontos de Ambiguidade|Estimativa de Pontos)|$)",
                Pattern.DOTALL);
        Matcher matcher1 = pattern1.matcher(aiResponse);
        if (matcher1.find()) {
            String result = matcher1.group(1).trim();
            if (!result.isEmpty() && result.length() >= 50 && !result.matches("^\\d+$")) {
                return result;
            }
        }
        int index = aiResponse.toLowerCase().indexOf("requisito refinado");
        if (index >= 0) {
            String after = aiResponse.substring(index + "requisito refinado".length()).replaceFirst("^\\s*:?\\*\\*?\\s*", "");
            int nextPts = after.toLowerCase().indexOf("pontos de ambiguidade");
            int nextEstimativa = after.toLowerCase().indexOf("estimativa de pontos");
            int next = -1;
            if (nextPts > 0 && nextEstimativa > 0) {
                next = Math.min(nextPts, nextEstimativa);
            } else if (nextEstimativa > 0) {
                next = nextEstimativa;
            } else if (nextPts > 0) {
                next = nextPts;
            }
            if (next > 0) {
                String result = after.substring(0, next).trim();
                if (result.length() >= 50 && !result.matches("^\\d+$")) {
                    return result;
                }
            } else {
                String result = after.trim();
                if (result.length() >= 50 && !result.matches("^\\d+$")) {
                    return result;
                }
            }
        }
        return null;
    }

    private List<String> extractAmbiguityWarnings(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Pontos de Ambiguidade\\s*:?\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*(?:Análise|Requisito Refinado|Estimativa de Pontos|Estimativa)|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            String section = matcher.group(1).trim();
            if (isNenhumAmbiguity(section)) {
                return List.of();
            }
            // Divide por bullet (-) ou número (1. 2.)
            String[] parts = section.split("(?=\\s*-\\s+|-\\s*\\[|\\d+\\.\\s+)");
            for (String part : parts) {
                String trimmed = sanitizePlainText(part).trim();
                if (trimmed.length() > 15 && !isNenhumAmbiguity(trimmed)) {
                    warnings.add(trimmed);
                }
            }
            if (warnings.isEmpty() && section.length() > 20 && !isNenhumAmbiguity(section)) {
                warnings.add(sanitizePlainText(section).trim());
            }
        }
        return warnings;
    }

    private boolean isNenhumAmbiguity(String text) {
        if (text == null || text.isBlank()) return true;
        String lower = text.toLowerCase().trim();
        if (lower.equals("nenhum") || lower.equals("nenhum.")) return true;
        if (lower.startsWith("nenhum.") || lower.startsWith("nenhum ")) return true;
        if (lower.length() < 35 && (lower.contains("não há") || lower.contains("nao ha") || lower.contains("sem ambiguidade"))) return true;
        return false;
    }

    @Transactional(readOnly = true)
    public RequirementReportDTO getGeneralReport(@NonNull UUID requirementSetId) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(requirementSetId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RequirementSet (projeto) não encontrado com ID: " + requirementSetId));
        List<Requirement> requirements = requirementRepository.findByRequirementSet_Id(requirementSetId);
        if (requirements.isEmpty()) {
            return new RequirementReportDTO(requirementSetId, requirementSet.getName(), List.of());
        }

        Map<UUID, Requirement> byId = requirements.stream()
                .collect(Collectors.toMap(Requirement::getUuid, requirement -> requirement));
        Map<UUID, List<ConflictCandidate>> candidatesByRequirement = new HashMap<>();
        int safeMaxPairs = Math.max(1, Math.min(maxSimilarPairs, 1000));
        similarityRepository.findSimilarPairs(requirementSetId, 0.85, safeMaxPairs)
                .forEach(pair -> {
                    Requirement left = byId.get(pair.leftId());
                    Requirement right = byId.get(pair.rightId());
                    if (left != null && right != null) {
                        candidatesByRequirement.computeIfAbsent(left.getUuid(), ignored -> new ArrayList<>())
                                .add(new ConflictCandidate(left, right, pair.score()));
                        candidatesByRequirement.computeIfAbsent(right.getUuid(), ignored -> new ArrayList<>())
                                .add(new ConflictCandidate(right, left, pair.score()));
                    }
                });

        List<RequirementReportDTO.RequirementReportItemDTO> items = new ArrayList<>();
        for (Requirement requirement : requirements) {
            List<RequirementReportDTO.ConflictInfo> conflicts = filterByIntent(
                    requirement,
                    candidatesByRequirement.getOrDefault(requirement.getUuid(), List.of()));
            boolean hasAmbiguity = requirement.getAmbiguityWarnings() != null
                    && !requirement.getAmbiguityWarnings().isEmpty();
            if (conflicts.isEmpty() && !hasAmbiguity) {
                continue;
            }
            List<String> problems = new ArrayList<>();
            List<String> resolutions = new ArrayList<>();
            if (!conflicts.isEmpty()) {
                problems.add("Possível duplicata ou conflito com " + conflicts.size() + " requisito(s)");
                resolutions.add("Revise os requisitos similares e consolide duplicatas quando necessário.");
            }
            if (hasAmbiguity) {
                problems.add("Ambiguidade identificada: " + requirement.getAmbiguityWarnings().size() + " ponto(s)");
                resolutions.add("Reescreva os pontos indicados com critérios objetivos e verificáveis.");
            }
            items.add(new RequirementReportDTO.RequirementReportItemDTO(
                    requirement.getUuid(), requirement.getRequirementId(),
                    requirement.getRefinedRequirement(), problems, conflicts, resolutions,
                    hasAmbiguity ? requirement.getAmbiguityWarnings() : null));
        }
        return new RequirementReportDTO(requirementSetId, requirementSet.getName(), items);
    }

    private record ConflictCandidate(Requirement req, Requirement conflicting, double score) {}

    /**
     * Filtro de intenção em lote: uma única chamada LLM para todos os pares.
     * Se desabilitado, retorna todos os candidatos como conflitos.
     */
    private List<RequirementReportDTO.ConflictInfo> filterByIntent(Requirement req, List<ConflictCandidate> candidates) {
        if (candidates.isEmpty()) return List.of();

        if (!intentionFilterEnabled) {
            return candidates.stream()
                    .map(c -> new RequirementReportDTO.ConflictInfo(
                            c.conflicting().getUuid(),
                            c.conflicting().getRequirementId(),
                            c.conflicting().getRefinedRequirement(),
                            c.score(),
                            "Avalie se os requisitos podem ser consolidados ou se um deles deve ser removido/alterado para evitar duplicação."))
                    .toList();
        }

        StringBuilder prompt = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            ConflictCandidate c = candidates.get(i);
            String t1 = (c.req().getRequirementId() != null ? c.req().getRequirementId() + ": " : "") + (c.req().getRefinedRequirement() != null ? c.req().getRefinedRequirement() : c.req().getRawRequirement());
            String t2 = (c.conflicting().getRequirementId() != null ? c.conflicting().getRequirementId() + ": " : "") + (c.conflicting().getRefinedRequirement() != null ? c.conflicting().getRefinedRequirement() : c.conflicting().getRawRequirement());
            prompt.append("Par ").append(i + 1).append(":\nREQ-A: ").append(t1).append("\nREQ-B: ").append(t2).append("\n\n");
        }
        prompt.append("Retorne uma linha por par (SIM ou NAO), na ordem Par 1, Par 2, etc.");

        try {
            String response = assistantAiService.verifySameIntentBatch(prompt.toString());
            String[] lines = response != null ? response.split("\\r?\\n") : new String[0];
            List<RequirementReportDTO.ConflictInfo> result = new ArrayList<>();
            String suggestion = "Avalie se os requisitos podem ser consolidados ou se um deles deve ser removido/alterado para evitar duplicação.";
            for (int i = 0; i < candidates.size(); i++) {
                ConflictCandidate c = candidates.get(i);
                String line = i < lines.length ? lines[i].trim().toLowerCase() : "";
                boolean sameIntent = line.isBlank() || (line.contains("sim") && !line.contains("nao") && !line.contains("não"));
                if (sameIntent) {
                    result.add(new RequirementReportDTO.ConflictInfo(
                            c.conflicting().getUuid(),
                            c.conflicting().getRequirementId(),
                            c.conflicting().getRefinedRequirement(),
                            c.score(),
                            suggestion));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Filtro de intenção em lote falhou: {}. Reportando todos como possíveis conflitos.", e.getMessage());
            return candidates.stream()
                    .map(c -> new RequirementReportDTO.ConflictInfo(
                            c.conflicting().getUuid(),
                            c.conflicting().getRequirementId(),
                            c.conflicting().getRefinedRequirement(),
                            c.score(),
                            "Avalie se os requisitos podem ser consolidados ou se um deles deve ser removido/alterado para evitar duplicação."))
                    .toList();
        }
    }
}
