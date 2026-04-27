package requirementsAssistantAI.application.service;

import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.ChatMessage;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceMember;
import requirementsAssistantAI.domain.WorkspaceRole;
import requirementsAssistantAI.dto.AddMemberRequest;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.ChatQuestionClusterDTO;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EmbeddingModel embeddingModel;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            ChatMessageRepository chatMessageRepository,
                            @Lazy EmbeddingModel embeddingModel) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.embeddingModel = embeddingModel;
    }

    @Transactional
    public WorkspaceDTO createWorkspace(@NonNull CreateWorkspaceRequest request, @NonNull String ownerEmail) {
        Workspace workspace = new Workspace(
                request.name(),
                request.description(),
                request.type(),
                ownerEmail
        );
        workspace.setInviteCode(generateInviteCode());
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember(workspace, ownerEmail, WorkspaceRole.OWNER);
        memberRepository.save(ownerMember);

        return toDTO(workspace, List.of(ownerMember));
    }

    @Transactional
    public WorkspaceDTO joinByInviteCode(@NonNull String code, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByInviteCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace com código", code));

        if (!memberRepository.existsByWorkspace_IdAndUserEmail(workspace.getId(), userEmail)) {
            WorkspaceMember member = new WorkspaceMember(workspace, userEmail, WorkspaceRole.MEMBER);
            memberRepository.save(member);
        }

        List<WorkspaceMember> members = memberRepository.findByWorkspace_Id(workspace.getId());
        return toDTO(workspace, members);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDTO> getMyWorkspaces(@NonNull String userEmail) {
        return workspaceRepository.findAllAccessibleByEmail(userEmail)
                .stream()
                .map(w -> toDTO(w, memberRepository.findByWorkspace_Id(w.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO getById(@NonNull UUID id, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        assertAccess(workspace, userEmail);
        return toDTO(workspace, workspace.getMembers());
    }

    @Transactional
    public WorkspaceDTO updateWorkspace(@NonNull UUID id, @NonNull CreateWorkspaceRequest request, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        assertAdminOrOwner(workspace, userEmail);

        workspace.setName(request.name());
        if (request.description() != null) workspace.setDescription(request.description());
        workspace.setType(request.type());
        workspace = workspaceRepository.save(workspace);
        return toDTO(workspace, workspace.getMembers());
    }

    @Transactional
    public void deleteWorkspace(@NonNull UUID id, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        assertOwner(workspace, userEmail);
        workspaceRepository.delete(workspace);
    }

    @Transactional
    public WorkspaceMemberDTO addMember(@NonNull UUID workspaceId, @NonNull AddMemberRequest request, @NonNull String requesterEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAdminOrOwner(workspace, requesterEmail);

        if (memberRepository.existsByWorkspace_IdAndUserEmail(workspaceId, request.userEmail())) {
            throw new IllegalStateException("Usuário " + request.userEmail() + " já é membro deste workspace.");
        }

        WorkspaceMember member = new WorkspaceMember(workspace, request.userEmail(), request.role());
        member = memberRepository.save(member);
        return toMemberDTO(member);
    }

    @Transactional
    public void removeMember(@NonNull UUID workspaceId, @NonNull String memberEmail, @NonNull String requesterEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAdminOrOwner(workspace, requesterEmail);

        if (workspace.getOwnerEmail().equals(memberEmail)) {
            throw new IllegalStateException("Não é possível remover o dono do workspace.");
        }
        memberRepository.deleteByWorkspace_IdAndUserEmail(workspaceId, memberEmail);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(@NonNull UUID workspaceId, @NonNull String requesterEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAccess(workspace, requesterEmail);

        return chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(workspaceId)
                .stream()
                .map(this::toChatMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatQuestionClusterDTO> getAnonymousQuestionRanking(@NonNull UUID workspaceId,
                                                                    @NonNull String requesterEmail,
                                                                    int limit,
                                                                    double similarityThreshold) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAdminOrOwner(workspace, requesterEmail);

        int safeLimit = Math.max(1, Math.min(limit, 50));
        double safeThreshold = Math.max(0.70, Math.min(similarityThreshold, 0.98));
        List<ChatMessage> messages = chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(workspaceId)
                .stream()
                .filter(cm -> cm.getQuestion() != null && !cm.getQuestion().isBlank())
                .toList();

        List<QuestionCluster> clusters = new ArrayList<>();
        for (ChatMessage message : messages) {
            String normalizedQuestion = normalizeQuestionForAnalytics(message.getQuestion());
            if (normalizedQuestion.isBlank()) {
                continue;
            }

            Embedding embedding = embeddingModel.embed(normalizedQuestion).content();
            float[] vector = embedding.vector();
            QuestionCluster bestCluster = null;
            double bestSimilarity = 0.0;

            for (QuestionCluster cluster : clusters) {
                double similarity = cosineSimilarity(vector, cluster.representativeVector);
                if (similarity >= safeThreshold && similarity > bestSimilarity) {
                    bestCluster = cluster;
                    bestSimilarity = similarity;
                }
            }

            if (bestCluster == null) {
                clusters.add(new QuestionCluster(message.getQuestion(), vector, message.getAskedAt()));
            } else {
                bestCluster.add(message.getQuestion(), message.getAskedAt(), bestSimilarity);
            }
        }

        clusters.sort(
                Comparator.comparingInt(QuestionCluster::size).reversed()
                        .thenComparing(QuestionCluster::lastAskedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        List<ChatQuestionClusterDTO> ranking = new ArrayList<>();
        for (int i = 0; i < Math.min(safeLimit, clusters.size()); i++) {
            QuestionCluster cluster = clusters.get(i);
            ranking.add(new ChatQuestionClusterDTO(
                    i + 1,
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

    public void assertAdminOrOwnerById(@NonNull UUID workspaceId, @NonNull String email) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAdminOrOwner(workspace, email);
    }

    public void assertMemberById(@NonNull UUID workspaceId, @NonNull String email) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAccess(workspace, email);
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void assertAccess(Workspace workspace, String email) {
        boolean isMember = workspace.getOwnerEmail().equals(email) ||
                workspace.getMembers().stream().anyMatch(m -> m.getUserEmail().equals(email));
        if (!isMember) throw new ForbiddenException("Acesso negado a este workspace.");
    }

    private void assertAdminOrOwner(Workspace workspace, String email) {
        boolean allowed = workspace.getOwnerEmail().equals(email) ||
                workspace.getMembers().stream().anyMatch(m ->
                        m.getUserEmail().equals(email) &&
                        (m.getRole() == WorkspaceRole.ADMIN || m.getRole() == WorkspaceRole.OWNER));
        if (!allowed) throw new ForbiddenException("Apenas admins ou o dono podem executar esta ação.");
    }

    private void assertOwner(Workspace workspace, String email) {
        if (!workspace.getOwnerEmail().equals(email))
            throw new ForbiddenException("Apenas o dono pode excluir o workspace.");
    }

    private WorkspaceDTO toDTO(Workspace workspace, List<WorkspaceMember> members) {
        List<WorkspaceMemberDTO> memberDTOs = members.stream()
                .map(this::toMemberDTO)
                .collect(Collectors.toList());
        return new WorkspaceDTO(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getType(),
                workspace.getOwnerEmail(),
                workspace.getInviteCode(),
                memberDTOs,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private WorkspaceMemberDTO toMemberDTO(WorkspaceMember m) {
        return new WorkspaceMemberDTO(m.getId(), m.getUserEmail(), m.getRole(), m.getCreatedAt());
    }

    private ChatMessageDTO toChatMessageDTO(requirementsAssistantAI.domain.ChatMessage cm) {
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

    private String normalizeQuestionForAnalytics(String question) {
        String normalized = Normalizer.normalize(question, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class QuestionCluster {
        private final String representativeQuestion;
        private final float[] representativeVector;
        private final List<String> questions = new ArrayList<>();
        private LocalDateTime firstAskedAt;
        private LocalDateTime lastAskedAt;
        private double similaritySum;
        private int similarityCount;

        QuestionCluster(String representativeQuestion, float[] representativeVector, LocalDateTime askedAt) {
            this.representativeQuestion = representativeQuestion;
            this.representativeVector = representativeVector;
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
            return uniqueQuestions.stream()
                    .limit(5)
                    .toList();
        }
    }
}
