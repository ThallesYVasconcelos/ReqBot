package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.ChatMessage;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import requirementsAssistantAI.application.ports.ChatAiService;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.ChatResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    @Value("${ai.chat.max-rag-results:3}")
    private int maxRagResults;
    @Value("${ai.chat.cache.enabled:true}")
    private boolean cacheEnabled;
    @Value("${ai.chat.cache.max-entries:100}")
    private int cacheMaxEntries;
    @Value("${ai.chat.cache.ttl-minutes:15}")
    private int cacheTtlMinutes;

    private final Map<String, CachedAnswer> answerCache = new ConcurrentHashMap<>();

    private final ChatbotConfigRepository chatbotConfigRepository;
    private final RequirementRepository requirementRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatAiService chatAiService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ChatService(
            ChatbotConfigRepository chatbotConfigRepository,
            RequirementRepository requirementRepository,
            ChatMessageRepository chatMessageRepository,
            ChatAiService chatAiService,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        this.chatbotConfigRepository = chatbotConfigRepository;
        this.requirementRepository = requirementRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatAiService = chatAiService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Responde a pergunta do usuário e persiste o histórico na tabela chat_messages.
     *
     * @param question   texto da pergunta
     * @param userEmail  e-mail do usuário autenticado (null em acesso público)
     */
    @Transactional
    public ChatResponseDTO answerQuestion(String question, String userEmail) {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                .orElseThrow(() -> new RuntimeException(
                        "Chatbot não está configurado ou está inativo. Configure através de /api/admin/chatbot/config"));

        if (!config.isAvailableNow()) {
            String unavailableMsg = "Desculpe, o chatbot está fora do horário de funcionamento. " +
                    "Horário de atendimento: " + formatTimeRange(config.getStartTime(), config.getEndTime());
            persistMessage(userEmail, question, unavailableMsg, false, false, config.getRequirementSet());
            return new ChatResponseDTO(unavailableMsg, question, LocalDateTime.now(), false, false);
        }

        RequirementSet requirementSet = config.getRequirementSet();
        Workspace workspace = requirementSet != null ? requirementSet.getWorkspace() : null;
        String projectId = requirementSet != null ? requirementSet.getId().toString() : null;
        String cacheKey = projectId + "||" + normalizeQuestion(question);

        if (cacheEnabled) {
            CachedAnswer cached = answerCache.get(cacheKey);
            if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
                persistMessage(userEmail, question, cached.answer, true, true, requirementSet);
                return new ChatResponseDTO(cached.answer, question, LocalDateTime.now(), true, true);
            }
        }

        try {
            String context = findRelevantContext(question, projectId);
            String rawAnswer = chatAiService.answerQuestion(question, context);
            String answer = (rawAnswer == null || rawAnswer.trim().isEmpty())
                    ? "Desculpe, não consegui processar sua pergunta. Por favor, tente novamente ou reformule sua pergunta."
                    : sanitizeAnswer(rawAnswer);

            if (cacheEnabled && rawAnswer != null && !rawAnswer.trim().isEmpty()) {
                evictIfNeeded();
                answerCache.put(cacheKey, new CachedAnswer(answer, LocalDateTime.now()));
            }

            persistMessage(userEmail, question, answer, false, true, requirementSet);
            return new ChatResponseDTO(answer, question, LocalDateTime.now(), true, true);

        } catch (Exception e) {
            String errorMsg = "Desculpe, ocorreu um erro ao processar sua pergunta. Por favor, tente novamente.";
            persistMessage(userEmail, question, errorMsg, false, true, requirementSet);
            return new ChatResponseDTO(errorMsg, question, LocalDateTime.now(), false, true);
        }
    }

    /** Mantém compatibilidade com chamadas sem userEmail (acesso anônimo). */
    @Transactional
    public ChatResponseDTO answerQuestion(String question) {
        return answerQuestion(question, null);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistoryByProject(UUID requirementSetId) {
        return chatMessageRepository
                .findByRequirementSet_IdOrderByAskedAtDesc(requirementSetId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistoryByUser(String userEmail) {
        return chatMessageRepository
                .findByUserEmailOrderByAskedAtDesc(userEmail)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private void persistMessage(String userEmail, String question, String answer,
                                boolean fromCache, boolean chatbotAvailable,
                                RequirementSet requirementSet) {
        try {
            Workspace workspace = requirementSet != null ? requirementSet.getWorkspace() : null;
            ChatMessage msg = new ChatMessage(
                    userEmail, question, answer, fromCache, chatbotAvailable, requirementSet, workspace
            );
            chatMessageRepository.save(msg);
        } catch (Exception ex) {
            // Persistência de histórico é best-effort — não deve derrubar a resposta ao usuário
        }
    }

    private String findRelevantContext(String userQuestion, String projectId) {
        if (projectId == null) return "Nenhum projeto configurado.";

        long totalRequirements = requirementRepository.countByRequirementSetId(UUID.fromString(projectId));
        if (totalRequirements == 0) {
            return "Nenhum requisito salvo encontrado para este projeto.";
        }

        Embedding queryEmbedding = embeddingModel.embed(userQuestion).content();
        EmbeddingSearchRequest semanticRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxRagResults)
                .minScore(0.6)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(semanticRequest);

        if (!result.matches().isEmpty()) {
            return result.matches().stream()
                    .map(m -> stripRequirementIdFromText(m.embedded().text()))
                    .collect(Collectors.joining("\n---\n"));
        }

        EmbeddingSearchRequest fallbackRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxRagResults)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .build();

        EmbeddingSearchResult<TextSegment> fallback = embeddingStore.search(fallbackRequest);
        if (!fallback.matches().isEmpty()) {
            return fallback.matches().stream()
                    .map(m -> stripRequirementIdFromText(m.embedded().text()))
                    .collect(Collectors.joining("\n---\n"));
        }

        return "Nenhum contexto relevante encontrado para a pergunta.";
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

    private ChatMessageDTO toDTO(ChatMessage cm) {
        return new ChatMessageDTO(
                cm.getId(),
                cm.getUserEmail(),
                cm.getQuestion(),
                cm.getAnswer(),
                cm.getAnsweredFromCache(),
                cm.getChatbotAvailable(),
                cm.getAskedAt(),
                cm.getRequirementSet() != null ? cm.getRequirementSet().getId() : null,
                cm.getRequirementSet() != null ? cm.getRequirementSet().getName() : null,
                cm.getWorkspace() != null ? cm.getWorkspace().getId() : null,
                cm.getWorkspace() != null ? cm.getWorkspace().getName() : null
        );
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
        if (startTime == null || endTime == null) return "24 horas";
        return startTime + " às " + endTime;
    }

    private String stripRequirementIdFromText(String text) {
        if (text == null) return "";
        return text.replaceFirst("^[A-Z]{2,3}-\\d+\\s*:\\s*", "").trim();
    }

    private String sanitizeAnswer(String answer) {
        if (answer == null) return "";
        return answer.replaceAll("\\b[A-Z]{2,4}-\\d+\\b", "[requisito]");
    }
}
