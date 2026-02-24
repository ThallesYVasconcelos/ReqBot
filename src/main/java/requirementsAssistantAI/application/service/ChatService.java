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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import requirementsAssistantAI.application.ports.ChatAiService;
import requirementsAssistantAI.dto.ChatResponseDTO;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    @Value("${ai.chat.max-rag-results:3}")
    private int maxRagResults;
    @Value("${ai.chat.cache.enabled:true}")
    private boolean cacheEnabled;
    @Value("${ai.chat.cache.max-entries:50}")
    private int cacheMaxEntries;
    @Value("${ai.chat.cache.ttl-minutes:10}")
    private int cacheTtlMinutes;

    private final Map<String, CachedAnswer> answerCache = new ConcurrentHashMap<>();

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
            String cacheKey = projectId + "||" + normalizeQuestion(question);
            if (cacheEnabled) {
                CachedAnswer cached = answerCache.get(cacheKey);
                if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
                    return new ChatResponseDTO(cached.answer, question, LocalDateTime.now(), true, true);
                }
            }
            String context = findRelevantContext(question, projectId);
            String rawAnswer = chatAiService.answerQuestion(question, context);
            String answer = (rawAnswer == null || rawAnswer.trim().isEmpty())
                    ? "Desculpe, não consegui processar sua pergunta. Por favor, tente novamente ou reformule sua pergunta."
                    : sanitizeAnswer(rawAnswer);
            if (cacheEnabled && rawAnswer != null && !rawAnswer.trim().isEmpty()) {
                evictIfNeeded();
                answerCache.put(cacheKey, new CachedAnswer(rawAnswer, LocalDateTime.now()));
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
        List<Requirement> savedRequirements = requirementRepository.findByRequirementSet_Id(
                UUID.fromString(projectId)
        );

        if (savedRequirements.isEmpty()) {
            return "Nenhum requisito salvo encontrado para este projeto.";
        }

        Embedding queryEmbedding = embeddingModel.embed(userQuestion).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxRagResults)
                .minScore(0.6)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        if (result.matches().isEmpty()) {
            int limit = Math.min(savedRequirements.size(), maxRagResults);
            return savedRequirements.stream()
                    .limit(limit)
                    .map(req -> stripRequirementId(req.getRefinedRequirement()))
                    .collect(Collectors.joining("\n---\n"));
        }

        return result.matches().stream()
                .map(m -> stripRequirementIdFromText(m.embedded().text()))
                .collect(Collectors.joining("\n---\n"));
    }

    private String normalizeQuestion(String q) {
        if (q == null) return "";
        return q.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void evictIfNeeded() {
        if (answerCache.size() >= cacheMaxEntries) {
            answerCache.entrySet().removeIf(e -> e.getValue().isExpired(cacheTtlMinutes));
            if (answerCache.size() >= cacheMaxEntries) {
                answerCache.clear();
            }
        }
    }

    private static class CachedAnswer {
        final String answer;
        final LocalDateTime createdAt;
        CachedAnswer(String answer, LocalDateTime createdAt) {
            this.answer = answer;
            this.createdAt = createdAt;
        }
        boolean isExpired(int ttlMinutes) {
            return createdAt.plusMinutes(ttlMinutes).isBefore(LocalDateTime.now());
        }
    }

    private String formatTimeRange(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return "24 horas";
        }
        return startTime + " às " + endTime;
    }

    private String stripRequirementId(String text) {
        if (text == null) return "";
        return text.replaceFirst("^[A-Z]{2,3}-\\d+\\s*:\\s*", "").trim();
    }

    private String stripRequirementIdFromText(String text) {
        if (text == null) return "";
        return stripRequirementId(text);
    }

    private String sanitizeAnswer(String answer) {
        if (answer == null) return "";
        return answer.replaceAll("\\b[A-Z]{2,4}-\\d+\\b", "[requisito]");
    }
}
