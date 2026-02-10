package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.RequirementRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import requirementsAssistantAI.application.ports.ChatAiService;
import requirementsAssistantAI.dto.ChatResponseDTO;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatbotConfigRepository chatbotConfigRepository;
    private final RequirementRepository requirementRepository;
    private final ChatAiService chatAiService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ChatService(
            ChatbotConfigRepository chatbotConfigRepository,
            RequirementRepository requirementRepository,
            ChatAiService chatAiService,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        this.chatbotConfigRepository = chatbotConfigRepository;
        this.requirementRepository = requirementRepository;
        this.chatAiService = chatAiService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Transactional(readOnly = true)
    public ChatResponseDTO answerQuestion(String question) {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                .orElseThrow(() -> new RuntimeException("Chatbot não está configurado ou está inativo. Configure o chatbot através do endpoint /api/admin/chatbot/config"));

        if (!config.isAvailableNow()) {
            return new ChatResponseDTO(
                    "Desculpe, o chatbot está fora do horário de funcionamento. " +
                    "Horário de atendimento: " + formatTimeRange(config.getStartTime(), config.getEndTime()),
                    question,
                    LocalDateTime.now(),
                    false,
                    false
            );
        }

        try {
            String projectId = config.getRequirementSet().getId().toString();
            String context = findRelevantContext(question, projectId);
            String answer = chatAiService.answerQuestion(question, context);
            if (answer == null || answer.trim().isEmpty()) {
                answer = "Desculpe, não consegui processar sua pergunta. Por favor, tente novamente ou reformule sua pergunta.";
            }
            return new ChatResponseDTO(answer, question, LocalDateTime.now(), true, true);
        } catch (Exception e) {
            return new ChatResponseDTO(
                    "Desculpe, ocorreu um erro ao processar sua pergunta. Por favor, tente novamente.",
                    question,
                    LocalDateTime.now(),
                    false,
                    true
            );
        }
    }

    private String findRelevantContext(String userQuestion, String projectId) {
        List<Requirement> approvedRequirements = requirementRepository.findByRequirementSet_Id(
                UUID.fromString(projectId)
        ).stream()
                .filter(req -> "APPROVED".equals(req.getStatus()))
                .collect(Collectors.toList());

        if (approvedRequirements.isEmpty()) {
            return "Nenhum requisito aprovado encontrado para este projeto.";
        }

        Embedding queryEmbedding = embeddingModel.embed(userQuestion).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.6)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        if (result.matches().isEmpty()) {
            return approvedRequirements.stream()
                    .map(req -> req.getRequirementId() + ": " + req.getRefinedRequirement())
                    .collect(Collectors.joining("\n---\n"));
        }

        return result.matches().stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n---\n"));
    }

    private String formatTimeRange(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return "24 horas";
        }
        return startTime + " às " + endTime;
    }
}
