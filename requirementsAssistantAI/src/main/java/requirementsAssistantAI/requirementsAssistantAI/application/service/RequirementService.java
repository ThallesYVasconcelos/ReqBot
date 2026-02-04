package requirementsAssistantAI.requirementsAssistantAI.application.service;

import requirementsAssistantAI.requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.requirementsAssistantAI.domain.RequirementHistory;
import requirementsAssistantAI.requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.requirementsAssistantAI.infrastructure.RequirementHistoryRepository;
import requirementsAssistantAI.requirementsAssistantAI.infrastructure.RequirementRepository;
import requirementsAssistantAI.requirementsAssistantAI.infrastructure.RequirementSetRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import requirementsAssistantAI.requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.requirementsAssistantAI.dto.RequirementDTO;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        String rawHash = computeSha256(rawRequirement);

        if (requirementRepository.existsByRequirementHashAndStatus(rawHash, STATUS_PENDING_APPROVAL)) {
            throw new IllegalStateException(
                    "Já existe um requisito idêntico (mesmo hash) com status PENDING_APPROVAL. Processamento abortado.");
        }

        String projectId = requirementSetId.toString();
        String relevantContext = findRelevantContext(rawRequirement, projectId);

        String aiResponse = assistantAiService.refineRequirement(rawRequirement, relevantContext);

        String requirementId = extractRequirementId(aiResponse);
        String analise = extractAnalise(aiResponse);
        String refinedRequirementText = extractRefinedRequirement(aiResponse);

        if ("REQ-UNKNOWN".equals(requirementId)) {
            requirementId = "REQ-TEMP-" + System.currentTimeMillis();
        }

        if (refinedRequirementText.isEmpty()) {
            analise = aiResponse;
            refinedRequirementText = "FALHA NO PARSE: " + rawRequirement;
        }

        Requirement requirement = new Requirement();
        requirement.setRequirementId(requirementId);
        requirement.setRefinedRequirement(refinedRequirementText);
        requirement.setAnalise(analise);
        requirement.setRequirementHash(rawHash);
        requirement.setStatus(STATUS_PENDING_APPROVAL);
        requirement.setRequirementSet(requirementSet);

        requirement = requirementRepository.save(requirement);

        RequirementHistory history = new RequirementHistory(requirement, "CREATED");
        requirementHistoryRepository.save(history);

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

        req.setStatus("APPROVED");
        requirementRepository.save(req);

        TextSegment segment = TextSegment.from(
                req.getRequirementId() + ": " + req.getRefinedRequirement(),
                metadata("project_id", req.getRequirementSet().getId().toString())
        );

        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
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

    private String findRelevantContext(String userQuery, String projectId) {
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.7)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        if (result.matches().isEmpty()) {
            return "Nenhum requisito anterior relevante encontrado.";
        }

        return result.matches().stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n---\n"));
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível", e);
        }
    }

    private String extractRequirementId(String aiResponse) {
        Pattern pattern = Pattern.compile("(REQ-\\d{4}-\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group(1) : "REQ-UNKNOWN";
    }

    private String extractAnalise(String aiResponse) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Análise(?:.*?):\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*Requisito Refinado|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group(1).trim() : "Verifique o texto completo.";
    }

    private String extractRefinedRequirement(String aiResponse) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\*\\*|##)?\\s*Requisito Refinado(?:.*?):\\*\\*?\\s*(.*?)(?=(?:\\*\\*|##)?\\s*(?:Critérios|Estimativa)|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
