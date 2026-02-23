package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.domain.RequirementHistory;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.infrastructure.RequirementHistoryRepository;
import requirementsAssistantAI.infrastructure.RequirementRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.dto.RequirementDTO;
import requirementsAssistantAI.dto.RequirementHistoryDTO;
import requirementsAssistantAI.dto.SaveRequirementRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import requirementsAssistantAI.dto.RequirementReportDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

    private final AssistantAiService assistantAiService;
    private final RequirementRepository requirementRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final RequirementHistoryRepository requirementHistoryRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public RequirementService(
            AssistantAiService assistantAiService,
            RequirementRepository requirementRepository,
            RequirementSetRepository requirementSetRepository,
            RequirementHistoryRepository requirementHistoryRepository,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        this.assistantAiService = assistantAiService;
        this.requirementRepository = requirementRepository;
        this.requirementSetRepository = requirementSetRepository;
        this.requirementHistoryRepository = requirementHistoryRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
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
        String relevantContext = findRelevantContext(rawRequirement, requirementSetId, requirementSetName, requirementSetDescription);

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

        addToEmbeddingStore(requirement);

        return convertToDTO(requirement);
    }

    private void addToEmbeddingStore(Requirement requirement) {
        String text = requirement.getRequirementId() + ": " + requirement.getRefinedRequirement();
        Metadata meta = Metadata.from(
                java.util.Map.of(
                        "project_id", requirement.getRequirementSet().getId().toString(),
                        "requirement_uuid", requirement.getUuid().toString()
                ));
        TextSegment segment = TextSegment.from(text, meta);
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
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
        addToEmbeddingStore(requirement);

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

    private String findRelevantContext(String userQuery, String projectId, String requirementSetName, String requirementSetDescription) {
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        StringBuilder contextBuilder = new StringBuilder();
        if (requirementSetName != null && !requirementSetName.trim().isEmpty()) {
            contextBuilder.append("PROJETO/CONJUNTO DE REQUISITOS: ").append(requirementSetName).append("\n");
        }
        if (requirementSetDescription != null && !requirementSetDescription.trim().isEmpty()) {
            contextBuilder.append("DESCRIÇÃO DO PROJETO: ").append(requirementSetDescription).append("\n\n");
        } else if (requirementSetName != null && !requirementSetName.trim().isEmpty()) {
            contextBuilder.append("\n");
        }

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxApprovedResults)
                .minScore(0.7)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        if (!result.matches().isEmpty()) {
            contextBuilder.append("REQUISITOS SALVOS NESTE PROJETO:\n");
            int usedLength = contextBuilder.length();
            for (var match : result.matches()) {
                String text = match.embedded().text();
                if (usedLength + text.length() + 5 > maxContextLength) {
                    int remaining = maxContextLength - usedLength - 5;
                    if (remaining > 50) {
                        contextBuilder.append(text, 0, remaining).append("\n...");
                    }
                    break;
                }
                if (usedLength > (requirementSetName != null ? requirementSetName.length() + 50 : 0)) {
                    contextBuilder.append("\n---\n");
                }
                contextBuilder.append(text);
                usedLength = contextBuilder.length();
            }
        } else {
            contextBuilder.append("Nenhum requisito anterior relevante encontrado neste projeto.\n");
        }

        return contextBuilder.toString();
    }

    private String generateNextRequirementId(@NonNull UUID requirementSetId) {
        List<Requirement> existing = requirementRepository.findByRequirementSet_Id(Objects.requireNonNull(requirementSetId));
        int maxNum = 0;
        Pattern pattern = Pattern.compile("REQ-(\\d+)(?:-|$)", Pattern.CASE_INSENSITIVE);
        for (Requirement r : existing) {
            String id = r.getRequirementId();
            if (id == null) continue;
            Matcher m = pattern.matcher(id);
            if (m.find()) {
                try {
                    int n = Integer.parseInt(m.group(1));
                    if (n > maxNum) maxNum = n;
                } catch (NumberFormatException ignored) {}
            }
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto) não encontrado com ID: " + requirementSetId));

        List<Requirement> requirements = requirementRepository.findByRequirementSet_Id(requirementSetId);
        List<RequirementReportDTO.RequirementReportItemDTO> itemsWithProblems = new ArrayList<>();

        double similarityThreshold = 0.85;

        for (Requirement req : requirements) {
            String queryText = req.getRequirementId() + ": " + req.getRefinedRequirement();
            Embedding queryEmbedding = embeddingModel.embed(queryText).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(20)
                    .minScore(similarityThreshold)
                    .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(requirementSetId.toString()))
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

            List<RequirementReportDTO.ConflictInfo> conflicts = new ArrayList<>();
            for (var match : result.matches()) {
                String matchedUuid = match.embedded().metadata().getString("requirement_uuid");
                if (matchedUuid != null && matchedUuid.equals(req.getUuid().toString())) {
                    continue;
                }
                if (queryText.equals(match.embedded().text())) {
                    continue;
                }
                String matchedText = match.embedded().text();
                double score = match.score();
                Requirement conflicting = findRequirementByText(requirements, matchedText);
                if (conflicting != null && isSameIntent(req, conflicting)) {
                    String suggestion = "Avalie se os requisitos podem ser consolidados ou se um deles deve ser removido/alterado para evitar duplicação.";
                    conflicts.add(new RequirementReportDTO.ConflictInfo(
                            conflicting.getUuid(),
                            conflicting.getRequirementId(),
                            conflicting.getRefinedRequirement(),
                            score,
                            suggestion
                    ));
                }
            }

            boolean hasConflicts = !conflicts.isEmpty();
            boolean hasAmbiguity = req.getAmbiguityWarnings() != null && !req.getAmbiguityWarnings().isEmpty();

            if (hasConflicts || hasAmbiguity) {
                List<String> problems = new ArrayList<>();
                List<String> resolutions = new ArrayList<>();
                if (hasConflicts) {
                    problems.add("Possível duplicata ou conflito com " + conflicts.size() + " requisito(s) similar(es)");
                    resolutions.add("Revise os requisitos listados e decida se devem ser consolidados.");
                    resolutions.add("Se forem duplicatas, mantenha apenas um e remova os demais.");
                    resolutions.add("Se forem complementares, reescreva para deixar clara a diferença.");
                }
                if (hasAmbiguity) {
                    problems.add("Ambiguidade identificada pela IA: " + req.getAmbiguityWarnings().size() + " ponto(s)");
                    resolutions.add("Revise os pontos de ambiguidade e implemente as sugestões indicadas.");
                    resolutions.add("Reescreva o requisito para deixar cada ponto mais claro e específico.");
                }
                itemsWithProblems.add(new RequirementReportDTO.RequirementReportItemDTO(
                        req.getUuid(),
                        req.getRequirementId(),
                        req.getRefinedRequirement(),
                        problems,
                        conflicts,
                        resolutions,
                        hasAmbiguity ? req.getAmbiguityWarnings() : null
                ));
            }
        }

        return new RequirementReportDTO(requirementSetId, requirementSet.getName(), itemsWithProblems);
    }

    private Requirement findRequirementByText(List<Requirement> requirements, String text) {
        if (text == null || text.length() < 10) return null;
        int colonIdx = text.indexOf(':');
        if (colonIdx <= 0) return null;
        String reqId = text.substring(0, colonIdx).trim();
        for (Requirement r : requirements) {
            if (reqId.equals(r.getRequirementId())) {
                return r;
            }
        }
        return null;
    }

    /**
     * Filtro de intenção: usa a LLM para verificar se dois requisitos têm a mesma intenção e ações.
     * Evita falsos positivos quando requisitos compartilham vocabulário mas são complementares
     * (ex: CRUD vs refino com IA).
     *
     * @return true se duplicata/conflito real; false se complementares ou em caso de erro (conservador).
     */
    private boolean isSameIntent(Requirement req1, Requirement req2) {
        String text1 = (req1.getRequirementId() != null ? req1.getRequirementId() + ": " : "") + (req1.getRefinedRequirement() != null ? req1.getRefinedRequirement() : req1.getRawRequirement());
        String text2 = (req2.getRequirementId() != null ? req2.getRequirementId() + ": " : "") + (req2.getRefinedRequirement() != null ? req2.getRefinedRequirement() : req2.getRawRequirement());
        String prompt = "REQ-A: " + text1 + "\nREQ-B: " + text2 + "\n\nEstes dois requisitos têm a mesma intenção e as mesmas ações principais? (SIM ou NAO)";
        try {
            String response = assistantAiService.verifySameIntent(prompt);
            if (response == null) return true;
            String r = response.trim().toLowerCase();
            if (r.contains("nao") || r.contains("não")) return false;
            return r.contains("sim");
        } catch (Exception e) {
            log.warn("Filtro de intenção falhou para {} vs {}: {}. Mantendo como possível conflito.", req1.getRequirementId(), req2.getRequirementId(), e.getMessage());
            return true;
        }
    }
}
