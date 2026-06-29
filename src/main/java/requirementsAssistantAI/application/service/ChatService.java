package requirementsAssistantAI.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.ports.ChatAiService;
import requirementsAssistantAI.domain.ChatMessage;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.ChatResponseDTO;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    @Value("${ai.chat.max-history-results:200}")
    private int maxHistoryResults;

    private final Map<String, CachedAnswer> answerCache = new ConcurrentHashMap<>();

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAiService chatAiService;
    private final ChatbotAccessService accessService;
    private final RagRetrievalService ragRetrievalService;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            ChatAiService chatAiService,
            ChatbotAccessService accessService,
            RagRetrievalService ragRetrievalService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatAiService = chatAiService;
        this.accessService = accessService;
        this.ragRetrievalService = ragRetrievalService;
    }

    @Transactional
    public ChatResponseDTO answerQuestion(
            String question, UUID userId, String userEmail, UUID chatbotId) {
        ChatbotConfig chatbot = accessService.requireChatAccess(chatbotId, userId);
        RequirementSet project = chatbot.getRequirementSet();

        if (!chatbot.isAvailableNow()) {
            String unavailable = "Desculpe, o chatbot está fora do horário de funcionamento. " +
                    "Horário de atendimento: " + formatTimeRange(chatbot.getStartTime(), chatbot.getEndTime());
            persistMessage(userEmail, question, unavailable, false, false, chatbot);
            return new ChatResponseDTO(unavailable, question, LocalDateTime.now(), false, false);
        }

        String projectId = project.getId().toString();
        // Chatbots do mesmo projeto compartilham respostas em cache e o mesmo RAG.
        String cacheKey = projectId + "||" + normalizeQuestion(question);
        if (cacheEnabled) {
            CachedAnswer cached = answerCache.get(cacheKey);
            if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
                persistMessage(userEmail, question, cached.answer, true, true, chatbot);
                return new ChatResponseDTO(cached.answer, question, LocalDateTime.now(), true, true);
            }
        }

        try {
            String context = ragRetrievalService.retrieve(
                    question, project.getId(), maxRagResults, 0.6, 2500);
            if (context.isBlank()) {
                context = "Nenhum requisito salvo ou contexto relevante encontrado para este projeto.";
            }
            String rawAnswer = chatAiService.answerQuestion(question, context);
            String answer = rawAnswer == null || rawAnswer.isBlank()
                    ? "Desculpe, não consegui processar sua pergunta. Tente reformulá-la."
                    : sanitizeAnswer(rawAnswer);
            if (cacheEnabled && rawAnswer != null && !rawAnswer.isBlank()) {
                evictIfNeeded();
                answerCache.put(cacheKey, new CachedAnswer(answer, LocalDateTime.now()));
            }
            persistMessage(userEmail, question, answer, false, true, chatbot);
            return new ChatResponseDTO(answer, question, LocalDateTime.now(), true, true);
        } catch (Exception exception) {
            String error = "Desculpe, ocorreu um erro ao processar sua pergunta. Por favor, tente novamente.";
            persistMessage(userEmail, question, error, false, true, chatbot);
            return new ChatResponseDTO(error, question, LocalDateTime.now(), false, true);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMyChatHistory(
            UUID chatbotId, UUID userId, String userEmail) {
        accessService.requireChatAccess(chatbotId, userId);
        return chatMessageRepository
                .findByChatbot_IdAndUserEmailOrderByAskedAtDesc(
                        chatbotId, userEmail, PageRequest.of(0, safeHistoryLimit()))
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistoryForManager(
            UUID chatbotId, UUID workspaceId, UUID requesterUserId) {
        accessService.requireManager(workspaceId, chatbotId, requesterUserId);
        return chatMessageRepository.findByChatbot_IdOrderByAskedAtDesc(
                        chatbotId, PageRequest.of(0, safeHistoryLimit()))
                .stream().map(this::toDTO).toList();
    }

    private void persistMessage(
            String userEmail, String question, String answer,
            boolean fromCache, boolean chatbotAvailable, ChatbotConfig chatbot) {
        try {
            RequirementSet project = chatbot.getRequirementSet();
            Workspace workspace = chatbot.getWorkspace();
            chatMessageRepository.save(new ChatMessage(
                    userEmail, question, answer, fromCache, chatbotAvailable,
                    project, workspace, chatbot));
        } catch (Exception ignored) {
            // Histórico é best-effort e não deve derrubar uma resposta válida.
        }
    }

    private ChatMessageDTO toDTO(ChatMessage message) {
        return new ChatMessageDTO(
                message.getId(), message.getUserEmail(), message.getQuestion(), message.getAnswer(),
                message.getAnsweredFromCache(), message.getChatbotAvailable(), message.getAskedAt(),
                message.getRequirementSet() != null ? message.getRequirementSet().getId() : null,
                message.getRequirementSet() != null ? message.getRequirementSet().getName() : null,
                message.getWorkspace() != null ? message.getWorkspace().getId() : null,
                message.getWorkspace() != null ? message.getWorkspace().getName() : null,
                message.getChatbot() != null ? message.getChatbot().getId() : null,
                message.getChatbot() != null ? message.getChatbot().getName() : null);
    }

    private String normalizeQuestion(String question) {
        return question == null ? "" : question.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void evictIfNeeded() {
        if (answerCache.size() >= cacheMaxEntries) {
            answerCache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheTtlMinutes));
            if (answerCache.size() >= cacheMaxEntries) {
                answerCache.clear();
            }
        }
    }

    private String formatTimeRange(java.time.LocalTime start, java.time.LocalTime end) {
        return start == null || end == null ? "24 horas" : start + " às " + end;
    }

    private String sanitizeAnswer(String answer) {
        return answer == null ? "" : answer.replaceAll("\\b[A-Z]{2,4}-\\d+\\b", "[requisito]");
    }

    private int safeHistoryLimit() {
        return Math.max(1, Math.min(maxHistoryResults, 1000));
    }

    private static class CachedAnswer {
        private final String answer;
        private final LocalDateTime createdAt;

        private CachedAnswer(String answer, LocalDateTime createdAt) {
            this.answer = answer;
            this.createdAt = createdAt;
        }

        private boolean isExpired(int ttlMinutes) {
            return createdAt.plusMinutes(ttlMinutes).isBefore(LocalDateTime.now());
        }
    }
}
