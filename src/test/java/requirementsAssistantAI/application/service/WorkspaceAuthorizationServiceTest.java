package requirementsAssistantAI.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.domain.*;
import requirementsAssistantAI.infrastructure.RequirementRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkspaceAuthorizationServiceTest {

    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository memberRepository;
    private WorkspaceAuthorizationService service;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        service = new WorkspaceAuthorizationService(
                workspaceRepository,
                memberRepository,
                mock(RequirementSetRepository.class),
                mock(RequirementRepository.class));
    }

    @Test
    void ownerAndAdminShouldHaveAdministrativeAccess() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, ownerId))
                .thenReturn(Optional.of(member(workspace, ownerId, WorkspaceRole.OWNER)));
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, adminId))
                .thenReturn(Optional.of(member(workspace, adminId, WorkspaceRole.ADMIN)));

        assertThat(service.requireOwnerOrAdmin(workspaceId, ownerId)).isSameAs(workspace);
        assertThat(service.requireOwnerOrAdmin(workspaceId, adminId)).isSameAs(workspace);
    }

    @Test
    void adminShouldNotPerformOwnerOnlyAction() {
        UUID workspaceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, adminId))
                .thenReturn(Optional.of(member(workspace, adminId, WorkspaceRole.ADMIN)));

        assertThatThrownBy(() -> service.requireOwner(workspaceId, adminId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void userWithoutMembershipShouldBeDenied() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace(workspaceId)));
        when(memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireOwnerOrAdmin(workspaceId, userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("não possui acesso");
    }

    private Workspace workspace(UUID id) {
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.PROFESSIONAL);
        workspace.setId(id);
        return workspace;
    }

    private WorkspaceMember member(Workspace workspace, UUID userId, WorkspaceRole role) {
        AppUser user = new AppUser("user@example.com", "Usuário", null, AuthRole.USER);
        user.setId(userId);
        return new WorkspaceMember(workspace, user, role);
    }
}
