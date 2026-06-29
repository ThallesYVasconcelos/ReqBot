package requirementsAssistantAI.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.domain.*;
import requirementsAssistantAI.dto.ChatQuestionClusterDTO;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkspaceServiceTest {

    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository memberRepository;
    private AppUserRepository appUserRepository;
    private WorkspaceAuthorizationService authorizationService;
    private ChatMessageRepository chatMessageRepository;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        authorizationService = mock(WorkspaceAuthorizationService.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        workspaceService = new WorkspaceService(
                workspaceRepository, memberRepository, appUserRepository,
                authorizationService, chatMessageRepository);
    }

    @Test
    void createWorkspaceShouldCreateOwnerMembershipFromAuthenticatedUser() {
        UUID ownerId = UUID.randomUUID();
        AppUser owner = user(ownerId, "professor@example.com");
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                "Engenharia de Software", "Projetos de requisitos", WorkspaceType.ACADEMIC);

        when(appUserRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            workspace.setId(UUID.randomUUID());
            workspace.setCreatedAt(LocalDateTime.now());
            return workspace;
        });
        when(memberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            member.setId(UUID.randomUUID());
            member.setCreatedAt(LocalDateTime.now());
            return member;
        });

        WorkspaceDTO result = workspaceService.createWorkspace(request, ownerId);

        assertThat(result.ownerEmail()).isEqualTo(owner.getEmail());
        assertThat(result.members()).singleElement().satisfies(member -> {
            assertThat(member.userEmail()).isEqualTo(owner.getEmail());
            assertThat(member.role()).isEqualTo(WorkspaceRole.OWNER);
        });
        verify(memberRepository).save(argThat(member ->
                member.getUser() == owner && member.getRole() == WorkspaceRole.OWNER));
    }

    @Test
    void anonymousRankingShouldGroupSimilarQuestionsWithoutExposingIdentity() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ChatMessage a = message("aluno-a@example.com", "Qual a cor da caixa do medicamento?", 10);
        ChatMessage b = message("aluno-b@example.com", "A caixa do medicamento muda de cor?", 11);
        ChatMessage c = message("aluno-c@example.com", "Como redefinir a senha?", 12);

        when(chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(
                eq(workspaceId), any(Pageable.class)))
                .thenReturn(List.of(c, b, a));

        List<ChatQuestionClusterDTO> result = workspaceService.getAnonymousQuestionRanking(
                workspaceId, ownerId, 10, 0.82);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).totalOccurrences()).isEqualTo(2);
        assertThat(result.get(0).similarQuestionsSample())
                .noneMatch(question -> question.contains("@example.com"));
        verify(authorizationService).requireOwnerOrAdmin(workspaceId, ownerId);
    }

    @Test
    void anonymousRankingShouldStopWhenWorkspaceAuthorizationFails() {
        UUID workspaceId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        doThrow(new ForbiddenException("Acesso administrativo negado"))
                .when(authorizationService).requireOwnerOrAdmin(workspaceId, outsiderId);

        assertThatThrownBy(() -> workspaceService.getAnonymousQuestionRanking(
                workspaceId, outsiderId, 10, 0.82))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(chatMessageRepository);
    }

    private AppUser user(UUID id, String email) {
        AppUser user = new AppUser(email, "Usuário", null, AuthRole.USER);
        user.setId(id);
        return user;
    }

    private ChatMessage message(String email, String question, int hour) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setUserEmail(email);
        message.setQuestion(question);
        message.setAskedAt(LocalDateTime.of(2026, 6, 27, hour, 0));
        return message;
    }
}
