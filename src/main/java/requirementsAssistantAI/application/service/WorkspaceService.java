package requirementsAssistantAI.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.AppUser;
import requirementsAssistantAI.domain.ChatMessage;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceMember;
import requirementsAssistantAI.domain.WorkspaceRole;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.ChatQuestionClusterDTO;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final AppUserRepository appUserRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final ChatMessageRepository chatMessageRepository;
    @Value("${ai.analytics.max-source-messages:500}")
    private int maxSourceMessages;
    @Value("${ai.analytics.max-history-results:200}")
    private int maxHistoryResults;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "as", "o", "os", "de", "da", "das", "do", "dos", "e", "em", "no", "na",
            "nos", "nas", "um", "uma", "para", "por", "com", "que", "qual", "como", "ao");

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            AppUserRepository appUserRepository,
            WorkspaceAuthorizationService authorizationService,
            ChatMessageRepository chatMessageRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
        this.authorizationService = authorizationService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public WorkspaceDTO createWorkspace(@NonNull CreateWorkspaceRequest request, @NonNull UUID ownerUserId) {
        AppUser owner = appUserRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", ownerUserId));
        Workspace workspace = workspaceRepository.save(
                new Workspace(request.name(), request.description(), request.type()));
        WorkspaceMember ownerMember = memberRepository.save(
                new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
        return toDTO(workspace, List.of(ownerMember));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDTO> getMyWorkspaces(@NonNull UUID userId) {
        return workspaceRepository.findAllAccessibleByUserId(userId)
                .stream()
                .map(workspace -> toDTO(workspace, memberRepository.findByWorkspace_Id(workspace.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO getById(@NonNull UUID id, @NonNull UUID userId) {
        authorizationService.requireOwnerOrAdmin(id, userId);
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        return toDTO(workspace, workspace.getMembers());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(@NonNull UUID workspaceId, @NonNull UUID requesterUserId) {
        authorizationService.requireOwnerOrAdmin(workspaceId, requesterUserId);
        return chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(
                        workspaceId, PageRequest.of(0, safeHistoryLimit()))
                .stream()
                .map(this::toChatMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatQuestionClusterDTO> getAnonymousQuestionRanking(
            @NonNull UUID workspaceId,
            @NonNull UUID requesterUserId,
            int limit,
            double similarityThreshold) {
        authorizationService.requireOwnerOrAdmin(workspaceId, requesterUserId);

        int safeLimit = Math.max(1, Math.min(limit, 50));
        double safeThreshold = Math.max(0.50, Math.min(similarityThreshold, 0.98));
        List<ChatMessage> messages = chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(
                        workspaceId, PageRequest.of(0, safeSourceLimit()))
                .stream()
                .filter(message -> message.getQuestion() != null && !message.getQuestion().isBlank())
                .toList();

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
                        .thenComparing(QuestionCluster::lastAskedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        );

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
                    safeThreshold
            ));
        }
        return ranking;
    }

    public void assertAdminOrOwnerById(@NonNull UUID workspaceId, @NonNull UUID userId) {
        authorizationService.requireOwnerOrAdmin(workspaceId, userId);
    }

    public void assertMemberById(@NonNull UUID workspaceId, @NonNull UUID userId) {
        authorizationService.requireOwnerOrAdmin(workspaceId, userId);
    }

    private WorkspaceDTO toDTO(Workspace workspace, List<WorkspaceMember> members) {
        List<WorkspaceMemberDTO> memberDTOs = members.stream()
                .map(this::toMemberDTO)
                .collect(Collectors.toList());
        String ownerEmail = members.stream()
                .filter(member -> member.getRole() == WorkspaceRole.OWNER)
                .map(member -> member.getUser().getEmail())
                .findFirst()
                .orElse(null);
        return new WorkspaceDTO(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getType(),
                ownerEmail,
                memberDTOs,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private WorkspaceMemberDTO toMemberDTO(WorkspaceMember member) {
        return new WorkspaceMemberDTO(
                member.getId(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getCreatedAt()
        );
    }

    private ChatMessageDTO toChatMessageDTO(ChatMessage message) {
        return new ChatMessageDTO(
                message.getId(),
                message.getUserEmail(),
                message.getQuestion(),
                message.getAnswer(),
                message.getAnsweredFromCache(),
                message.getChatbotAvailable(),
                message.getAskedAt(),
                message.getRequirementSet() != null ? message.getRequirementSet().getId() : null,
                message.getRequirementSet() != null ? message.getRequirementSet().getName() : null,
                message.getWorkspace() != null ? message.getWorkspace().getId() : null,
                message.getWorkspace() != null ? message.getWorkspace().getName() : null,
                message.getChatbot() != null ? message.getChatbot().getId() : null,
                message.getChatbot() != null ? message.getChatbot().getName() : null
        );
    }

    private String normalizeQuestionForAnalytics(String question) {
        return Normalizer.normalize(question, Normalizer.Form.NFD)
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

    private int safeSourceLimit() {
        return Math.max(20, Math.min(maxSourceMessages, 1000));
    }

    private int safeHistoryLimit() {
        return Math.max(1, Math.min(maxHistoryResults, 1000));
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
