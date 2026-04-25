package requirementsAssistantAI.application.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.domain.ChatMessage;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceMember;
import requirementsAssistantAI.domain.WorkspaceRole;
import requirementsAssistantAI.domain.WorkspaceType;
import requirementsAssistantAI.dto.AddMemberRequest;
import requirementsAssistantAI.dto.ChatQuestionClusterDTO;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {

    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository memberRepository;
    private ChatMessageRepository chatMessageRepository;
    private EmbeddingModel embeddingModel;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        embeddingModel = mock(EmbeddingModel.class);
        workspaceService = new WorkspaceService(
                workspaceRepository,
                memberRepository,
                chatMessageRepository,
                embeddingModel
        );
    }

    @Test
    void createWorkspaceShouldCreateOwnerMembership() {
        String ownerEmail = "professor@example.com";
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                "Engenharia de Software",
                "Turma de requisitos",
                WorkspaceType.ACADEMIC
        );

        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            workspace.setId(UUID.randomUUID());
            workspace.setCreatedAt(LocalDateTime.now());
            workspace.setUpdatedAt(LocalDateTime.now());
            return workspace;
        });
        when(memberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            member.setId(UUID.randomUUID());
            member.setCreatedAt(LocalDateTime.now());
            return member;
        });

        WorkspaceDTO result = workspaceService.createWorkspace(request, ownerEmail);

        assertThat(result.name()).isEqualTo("Engenharia de Software");
        assertThat(result.type()).isEqualTo(WorkspaceType.ACADEMIC);
        assertThat(result.ownerEmail()).isEqualTo(ownerEmail);
        assertThat(result.members())
                .extracting(WorkspaceMemberDTO::userEmail)
                .containsExactly(ownerEmail);
        assertThat(result.members())
                .extracting(WorkspaceMemberDTO::role)
                .containsExactly(WorkspaceRole.OWNER);
        verify(workspaceRepository).save(any(Workspace.class));
        verify(memberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void addMemberShouldRequireAdminOrOwner() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId, "owner@example.com");
        workspace.getMembers().add(new WorkspaceMember(workspace, "student@example.com", WorkspaceRole.MEMBER));

        when(workspaceRepository.findByIdWithMembers(workspaceId)).thenReturn(Optional.of(workspace));

        AddMemberRequest request = new AddMemberRequest("new@example.com", WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> workspaceService.addMember(workspaceId, request, "student@example.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Apenas admins ou o dono");
    }

    @Test
    void anonymousQuestionRankingShouldGroupSimilarQuestionsWithoutStudentIdentity() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId, "professor@example.com");
        workspace.getMembers().add(new WorkspaceMember(workspace, "professor@example.com", WorkspaceRole.OWNER));

        ChatMessage questionA = chatMessage(
                "student-a@example.com",
                "Um medicamento sempre terá a mesma cor de caixa?",
                LocalDateTime.of(2026, 4, 20, 10, 0)
        );
        ChatMessage questionB = chatMessage(
                "student-b@example.com",
                "A cor da caixa do medicamento muda dependendo do fabricante?",
                LocalDateTime.of(2026, 4, 20, 10, 5)
        );
        ChatMessage questionC = chatMessage(
                "student-c@example.com",
                "O usuário pode redefinir a senha pelo e-mail?",
                LocalDateTime.of(2026, 4, 20, 11, 0)
        );

        when(workspaceRepository.findByIdWithMembers(workspaceId)).thenReturn(Optional.of(workspace));
        when(chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(workspaceId))
                .thenReturn(List.of(questionC, questionB, questionA));
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0, String.class);
            if (text.contains("medicamento") || text.contains("caixa") || text.contains("fabricante")) {
                return Response.from(Embedding.from(new float[]{1.0f, 0.0f, 0.0f}));
            }
            return Response.from(Embedding.from(new float[]{0.0f, 1.0f, 0.0f}));
        });

        List<ChatQuestionClusterDTO> ranking = workspaceService.getAnonymousQuestionRanking(
                workspaceId,
                "professor@example.com",
                10,
                0.82
        );

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).rank()).isEqualTo(1);
        assertThat(ranking.get(0).totalOccurrences()).isEqualTo(2);
        assertThat(ranking.get(0).similarQuestionsSample())
                .contains(
                        "Um medicamento sempre terá a mesma cor de caixa?",
                        "A cor da caixa do medicamento muda dependendo do fabricante?"
                );
        assertThat(ranking.get(0).similarQuestionsSample())
                .noneMatch(sample -> sample.contains("@example.com"));
        assertThat(ranking.get(1).totalOccurrences()).isEqualTo(1);
    }

    @Test
    void anonymousQuestionRankingShouldBeRestrictedToAdminOrOwner() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId, "owner@example.com");
        workspace.getMembers().add(new WorkspaceMember(workspace, "student@example.com", WorkspaceRole.MEMBER));

        when(workspaceRepository.findByIdWithMembers(workspaceId)).thenReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.getAnonymousQuestionRanking(
                workspaceId,
                "student@example.com",
                10,
                0.82
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Apenas admins ou o dono");
    }

    private Workspace workspace(UUID id, String ownerEmail) {
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.ACADEMIC, ownerEmail);
        workspace.setId(id);
        workspace.setCreatedAt(LocalDateTime.now());
        workspace.setUpdatedAt(LocalDateTime.now());
        return workspace;
    }

    private ChatMessage chatMessage(String userEmail, String question, LocalDateTime askedAt) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setUserEmail(userEmail);
        message.setQuestion(question);
        message.setAnswer("Resposta");
        message.setAskedAt(askedAt);
        message.setAnsweredFromCache(false);
        message.setChatbotAvailable(true);
        return message;
    }
}
