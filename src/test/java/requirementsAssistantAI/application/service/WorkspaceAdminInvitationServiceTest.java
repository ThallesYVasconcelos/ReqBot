package requirementsAssistantAI.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import requirementsAssistantAI.domain.*;
import requirementsAssistantAI.dto.WorkspaceAdminInvitationDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.infrastructure.WorkspaceAdminInvitationRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkspaceAdminInvitationServiceTest {

    private WorkspaceAuthorizationService authorizationService;
    private WorkspaceAdminInvitationRepository invitationRepository;
    private WorkspaceMemberRepository memberRepository;
    private AppUserRepository appUserRepository;
    private WorkspaceAdminInvitationService service;

    @BeforeEach
    void setUp() {
        authorizationService = mock(WorkspaceAuthorizationService.class);
        invitationRepository = mock(WorkspaceAdminInvitationRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        service = new WorkspaceAdminInvitationService(
                authorizationService, invitationRepository, memberRepository, appUserRepository);
    }

    @Test
    void inviteShouldPersistOnlyTokenHash() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId);
        AppUser owner = user(ownerId, "owner@example.com");
        when(authorizationService.requireOwner(workspaceId, ownerId)).thenReturn(workspace);
        when(appUserRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.findByWorkspace_IdAndInvitedEmailIgnoreCaseAndStatus(
                workspaceId, "admin@example.com", InvitationStatus.PENDING)).thenReturn(Optional.empty());
        when(invitationRepository.save(any())).thenAnswer(invocation -> {
            WorkspaceAdminInvitation invitation = invocation.getArgument(0);
            invitation.setId(UUID.randomUUID());
            return invitation;
        });

        WorkspaceAdminInvitationDTO result = service.invite(
                workspaceId, " ADMIN@example.com ", ownerId);

        ArgumentCaptor<WorkspaceAdminInvitation> captor =
                ArgumentCaptor.forClass(WorkspaceAdminInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(result.token()).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(result.token());
        assertThat(captor.getValue().getInvitedEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void acceptShouldCreateAdminMembershipAndConsumeInvitation() throws Exception {
        String rawToken = "token-secreto";
        UUID workspaceId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Workspace workspace = workspace(workspaceId);
        AppUser admin = user(adminId, "admin@example.com");
        WorkspaceAdminInvitation invitation = new WorkspaceAdminInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setWorkspace(workspace);
        invitation.setInvitedEmail(admin.getEmail());
        invitation.setTokenHash(hash(rawToken));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(invitationRepository.findByTokenHashAndStatus(hash(rawToken), InvitationStatus.PENDING))
                .thenReturn(Optional.of(invitation));
        when(appUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(memberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, adminId)).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            member.setId(UUID.randomUUID());
            member.setCreatedAt(LocalDateTime.now());
            return member;
        });

        WorkspaceMemberDTO result = service.accept(rawToken, adminId);

        assertThat(result.role()).isEqualTo(WorkspaceRole.ADMIN);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedBy()).isSameAs(admin);
        verify(invitationRepository).save(invitation);
    }

    private Workspace workspace(UUID id) {
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.PROFESSIONAL);
        workspace.setId(id);
        return workspace;
    }

    private AppUser user(UUID id, String email) {
        AppUser user = new AppUser(email, "Usuário", null, AuthRole.USER);
        user.setId(id);
        return user;
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
