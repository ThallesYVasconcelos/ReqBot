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
import requirementsAssistantAI.dto.ChatQuestionClusterDTO;
import requirementsAssistantAI.dto.ChatResponseDTO;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Value("${ai.analytics.max-source-messages:500}")
    private int maxSourceMessages;

    private final Map<String, CachedAnswer> answerCache = new ConcurrentHashMap<>();

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "as", "o", "os", "de", "da", "das", "do", "dos", "e", "em", "no", "na",
            "nos", "nas", "um", "uma", "para", "por", "com", "que", "qual", "como", "ao");

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

    @Transactional(readOnly = true)
    public List<ChatQuestionClusterDTO> getQuestionRankingForManager(
            UUID workspaceId,
            UUID chatbotId,
            UUID requesterUserId,
            int limit,
            double similarityThreshold) {
        accessService.requireManager(workspaceId, chatbotId, requesterUserId);
        return rankQuestions(
                chatMessageRepository.findQuestionsByChatbotIdOrderByAskedAtDesc(
                        chatbotId, PageRequest.of(0, safeSourceLimit())),
                limit,
                similarityThreshold);
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

    private int safeSourceLimit() {
        return Math.max(20, Math.min(maxSourceMessages, 1000));
    }

    private List<ChatQuestionClusterDTO> rankQuestions(
            List<ChatMessage> messages,
            int limit,
            double similarityThreshold) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        double safeThreshold = Math.max(0.50, Math.min(similarityThreshold, 0.98));
        List<QuestionCluster> clusters = new ArrayList<>();
        for (ChatMessage message : messages) {
            String normalizedQuestion = normalizeQuestionForAnalytics(message.getQuestion());
            if (normalizedQuestion.isBlank()) {
                continue;
            }
            Set<String> tokens = significantTokens(normalizedQuestion);
            QuestionCluster bestCluster = null;
            double bestSimilarity = 0.0;
            for (QuestionCluster cluster : clusters) {
                double similarity = lexicalSimilarity(
                        normalizedQuestion, tokens,
                        cluster.normalizedRepresentative, cluster.representativeTokens);
                if (similarity >= safeThreshold && similarity > bestSimilarity) {
                    bestCluster = cluster;
                    bestSimilarity = similarity;
                }
            }
            if (bestCluster == null) {
                clusters.add(new QuestionCluster(
                        message.getQuestion(), normalizedQuestion, tokens, message.getAskedAt()));
            } else {
                bestCluster.add(message.getQuestion(), message.getAskedAt(), bestSimilarity);
            }
        }
        clusters.sort(
                Comparator.comparingInt(QuestionCluster::size).reversed()
                        .thenComparing(QuestionCluster::lastAskedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        List<ChatQuestionClusterDTO> ranking = new ArrayList<>();
        for (int index = 0; index < Math.min(safeLimit, clusters.size()); index++) {
            QuestionCluster cluster = clusters.get(index);
            ranking.add(new ChatQuestionClusterDTO(
                    index + 1,
                    cluster.representativeQuestion,
                    cluster.size(),
                    cluster.sampleQuestions(),
                    cluster.firstAskedAt,
                    cluster.lastAskedAt,
                    cluster.averageSimilarity(),
                    safeThreshold));
        }
        return ranking;
    }

    private String normalizeQuestionForAnalytics(String question) {
        return question == null ? "" : Normalizer.normalize(question, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> significantTokens(String normalizedQuestion) {
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(normalizedQuestion.split("\\s+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .forEach(tokens::add);
        return tokens;
    }

    private double lexicalSimilarity(
            String firstText, Set<String> first,
            String secondText, Set<String> second) {
        if (firstText.equals(secondText)) {
            return 1.0;
        }
        if (first.isEmpty() || second.isEmpty()) {
            return 0.0;
        }
        long intersection = first.stream().filter(second::contains).count();
        int union = first.size() + second.size() - (int) intersection;
        double jaccard = union == 0 ? 0.0 : intersection / (double) union;
        double containment = intersection / (double) Math.min(first.size(), second.size());
        return 0.6 * containment + 0.4 * jaccard;
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

    private static class QuestionCluster {
        private final String representativeQuestion;
        private final String normalizedRepresentative;
        private final Set<String> representativeTokens;
        private final List<String> questions = new ArrayList<>();
        private LocalDateTime firstAskedAt;
        private LocalDateTime lastAskedAt;
        private double similaritySum;
        private int similarityCount;

        QuestionCluster(
                String representativeQuestion,
                String normalizedRepresentative,
                Set<String> representativeTokens,
                LocalDateTime askedAt) {
            this.representativeQuestion = representativeQuestion;
            this.normalizedRepresentative = normalizedRepresentative;
            this.representativeTokens = Set.copyOf(representativeTokens);
            this.questions.add(representativeQuestion);
            this.firstAskedAt = askedAt;
            this.lastAskedAt = askedAt;
            this.similaritySum = 1.0;
            this.similarityCount = 1;
        }

        void add(String question, LocalDateTime askedAt, double similarity) {
            questions.add(question);
            if (askedAt != null && (firstAskedAt == null || askedAt.isBefore(firstAskedAt))) {
                firstAskedAt = askedAt;
            }
            if (askedAt != null && (lastAskedAt == null || askedAt.isAfter(lastAskedAt))) {
                lastAskedAt = askedAt;
            }
            similaritySum += similarity;
            similarityCount++;
        }

        int size() {
            return questions.size();
        }

        LocalDateTime lastAskedAt() {
            return lastAskedAt;
        }

        double averageSimilarity() {
            return similarityCount == 0 ? 0.0 : similaritySum / similarityCount;
        }

        List<String> sampleQuestions() {
            Set<String> uniqueQuestions = new LinkedHashSet<>(questions);
            return uniqueQuestions.stream().limit(5).toList();
        }
    }
}
