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
import requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.dto.RequirementDTO;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.langchain4j.data.document.Metadata.metadata;

@Service
public class RequirementService {

    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_CONFLICT = "CONFLICT";

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

    @Transactional
    public Requirement processAndSaveRequirement(String rawRequirement, @NonNull UUID requirementSetId) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(requirementSetId))
                .orElseThrow(() -> new RuntimeException("RequirementSet não encontrado"));

        String relevantContext = findRelevantContext(rawRequirement, requirementSetId.toString(), requirementSet.getName());
        
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
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                aiResponse = null;
                break;
            }
        }

        boolean isAiResponseValid = aiResponse != null && aiResponse.length() >= 200;
        boolean isPartialProcessing = aiResponse == null || !isAiResponseValid;
        
        String requirementId = isPartialProcessing ? "REQ-TEMP-" + System.currentTimeMillis() : extractRequirementId(aiResponse);
        String analise = isPartialProcessing ? null : extractAnalise(aiResponse);
        String refinedRequirementText = isPartialProcessing ? null : extractRefinedRequirement(aiResponse);
        boolean hasConflict = isPartialProcessing ? false : hasConflictDetected(aiResponse);

        if (requirementId.equals("REQ-UNKNOWN")) {
            requirementId = "REQ-TEMP-" + System.currentTimeMillis();
        }

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

        Requirement requirement = new Requirement();
        requirement.setRequirementId(requirementId);
        requirement.setRefinedRequirement(refinedRequirementText);
        requirement.setAnalise(analise);
        requirement.setStatus(hasConflict ? STATUS_CONFLICT : STATUS_PENDING_APPROVAL);
        requirement.setRequirementSet(requirementSet);

        requirement = requirementRepository.save(requirement);
        requirementHistoryRepository.save(new RequirementHistory(requirement, "CREATED"));

        return requirement;
    }

    @Transactional
    public RequirementDTO processAndSaveRequirementAsDTO(String rawRequirement, @NonNull UUID requirementSetId) {
        Requirement requirement = processAndSaveRequirement(rawRequirement, requirementSetId);
        return convertToDTO(requirement);
    }

    public RequirementDTO getRequirementById(@NonNull UUID id) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Requirement não encontrado com ID: " + id));
        return convertToDTO(requirement);
    }

    public List<RequirementDTO> getRequirementsBySetId(@NonNull UUID requirementSetId) {
        List<Requirement> requirements = requirementRepository.findByRequirementSet_Id(Objects.requireNonNull(requirementSetId));
        return requirements.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<RequirementHistory> getRequirementHistory(@NonNull UUID requirementId) {
        return requirementHistoryRepository.findByRequirement_UuidOrderByCreatedAtDesc(Objects.requireNonNull(requirementId));
    }

    @Transactional
    public void approveRequirement(@NonNull UUID requirementUuid) {
        Requirement req = requirementRepository.findById(Objects.requireNonNull(requirementUuid))
                .orElseThrow(() -> new RuntimeException("Requirement não encontrado com ID: " + requirementUuid));

        if (STATUS_CONFLICT.equals(req.getStatus())) {
            throw new IllegalStateException("Não é possível aprovar um requisito com status CONFLICT. Resolva o conflito primeiro.");
        }

        req.setStatus("APPROVED");
        requirementRepository.save(req);

        TextSegment segment = TextSegment.from(
                req.getRequirementId() + ": " + req.getRefinedRequirement(),
                metadata("project_id", req.getRequirementSet().getId().toString())
        );

        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
    }

    @Transactional
    public RequirementDTO updatePendingRequirement(@NonNull UUID requirementUuid, String rawRequirement) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(requirementUuid))
                .orElseThrow(() -> new RuntimeException("Requirement não encontrado com ID: " + requirementUuid));

        if (!STATUS_PENDING_APPROVAL.equals(requirement.getStatus()) && !STATUS_CONFLICT.equals(requirement.getStatus())) {
            throw new IllegalStateException("Apenas requisitos com status PENDING_APPROVAL ou CONFLICT podem ser atualizados");
        }

        UUID requirementSetId = requirement.getRequirementSet().getId();
        String requirementSetName = requirement.getRequirementSet().getName();
        String relevantContext = findRelevantContext(rawRequirement, requirementSetId.toString(), requirementSetName);
        
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
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                aiResponse = null;
                break;
            }
        }

        boolean isAiResponseValid = aiResponse != null && aiResponse.length() >= 200;
        boolean isPartialProcessing = aiResponse == null || !isAiResponseValid;
        
        String requirementId = isPartialProcessing ? "REQ-TEMP-" + System.currentTimeMillis() : extractRequirementId(aiResponse);
        String analise = isPartialProcessing ? null : extractAnalise(aiResponse);
        String refinedRequirementText = isPartialProcessing ? null : extractRefinedRequirement(aiResponse);
        boolean hasConflict = isPartialProcessing ? false : hasConflictDetected(aiResponse);

        if (requirementId.equals("REQ-UNKNOWN")) {
            requirementId = "REQ-TEMP-" + System.currentTimeMillis();
        }

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

        requirement.setRequirementId(requirementId);
        requirement.setRefinedRequirement(refinedRequirementText);
        requirement.setAnalise(analise);
        requirement.setStatus(hasConflict ? STATUS_CONFLICT : STATUS_PENDING_APPROVAL);

        requirement = requirementRepository.save(requirement);
        requirementHistoryRepository.save(new RequirementHistory(requirement, "UPDATED"));

        return convertToDTO(requirement);
    }

    @Transactional
    public void deleteRequirement(@NonNull UUID requirementUuid) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(requirementUuid))
                .orElseThrow(() -> new RuntimeException("Requirement não encontrado com ID: " + requirementUuid));

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
                requirement.getRefinedRequirement(),
                requirement.getAnalise(),
                requirement.getStatus(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt(),
                requirementSet != null ? requirementSet.getId() : null,
                requirementSet != null ? requirementSet.getName() : null
        );
    }

    private String findRelevantContext(String userQuery, String projectId, String requirementSetName) {
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        StringBuilder contextBuilder = new StringBuilder();
        
        if (requirementSetName != null && !requirementSetName.trim().isEmpty()) {
            contextBuilder.append("PROJETO/CONJUNTO DE REQUISITOS: ").append(requirementSetName).append("\n\n");
        }

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.7)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        if (!result.matches().isEmpty()) {
            contextBuilder.append("REQUISITOS JÁ APROVADOS NESTE PROJETO:\n");
            final int MAX_CONTEXT_LENGTH = 3000;
            int usedLength = contextBuilder.length();
            
            for (var match : result.matches()) {
                String text = match.embedded().text();
                if (usedLength + text.length() + 5 > MAX_CONTEXT_LENGTH) {
                    int remaining = MAX_CONTEXT_LENGTH - usedLength - 5;
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

    private String extractRequirementId(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "REQ-UNKNOWN";
        }
        Pattern pattern1 = Pattern.compile("(REQ-\\d{4}-\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(aiResponse);
        if (matcher1.find()) {
            return matcher1.group(1);
        }
        Pattern pattern2 = Pattern.compile("(REQ-\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(aiResponse);
        if (matcher2.find()) {
            return matcher2.group(1);
        }
        Pattern pattern3 = Pattern.compile("(REQ-\\d+)\\s*-", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pattern3.matcher(aiResponse);
        if (matcher3.find()) {
            return matcher3.group(1);
        }
        return "REQ-UNKNOWN";
    }

    private String extractAnalise(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return null;
        }
        if (aiResponse.length() < 50) {
            return null;
        }
        Pattern pattern1 = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Análise\\s*:?\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*(?:Requisito Refinado|Status|Estimativa)|$)",
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
            int next = Math.min(
                after.toLowerCase().indexOf("requisito refinado"),
                after.toLowerCase().indexOf("estimativa")
            );
            if (next > 0) {
                String result = after.substring(0, next).trim();
                if (result.length() >= 30 && !result.matches("^\\d+$")) {
                    return result;
                }
            }
        }
        return null;
    }

    private String extractRefinedRequirement(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return null;
        }
        if (aiResponse.length() < 50) {
            return null;
        }
        
        Pattern pattern1 = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Requisito Refinado\\s*:?\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*Estimativa de Pontos|$)",
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
            String after = aiResponse.substring(index + "requisito refinado".length())
                .replaceFirst("^\\s*:?\\*\\*?\\s*", "");
            
            int nextEstimativa = after.toLowerCase().indexOf("estimativa de pontos");
            int nextStatus = after.toLowerCase().indexOf("status de validação");
            
            int next = -1;
            if (nextEstimativa > 0 && nextStatus > 0) {
                next = Math.min(nextEstimativa, nextStatus);
            } else if (nextEstimativa > 0) {
                next = nextEstimativa;
            } else if (nextStatus > 0) {
                next = nextStatus;
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

    private boolean hasConflictDetected(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Status de Validação\\s*:?\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*(?:Análise|Requisito Refinado|$))",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            String status = matcher.group(1).trim();
            return status.toUpperCase().contains("CONFLITO") || status.toUpperCase().contains("CONFLICT");
        }
        
        return aiResponse.toUpperCase().contains("CONFLITO DETECTADO") || 
               aiResponse.toUpperCase().contains("CONFLICT DETECTED");
    }
}
