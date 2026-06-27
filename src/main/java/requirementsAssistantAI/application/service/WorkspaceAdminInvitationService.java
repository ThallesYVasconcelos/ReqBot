package requirementsAssistantAI.application.service;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.*;
import requirementsAssistantAI.dto.WorkspaceAdminInvitationDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.infrastructure.WorkspaceAdminInvitationRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceAdminInvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRATION_HOURS = 72;

    private final WorkspaceAuthorizationService authorizationService;
    private final WorkspaceAdminInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final AppUserRepository appUserRepository;

    public WorkspaceAdminInvitationService(
            WorkspaceAuthorizationService authorizationService,
            WorkspaceAdminInvitationRepository invitationRepository,
            WorkspaceMemberRepository memberRepository,
            AppUserRepository appUserRepository) {
        this.authorizationService = authorizationService;
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public WorkspaceAdminInvitationDTO invite(
            @NonNull UUID workspaceId,
            @NonNull String invitedEmail,
            @NonNull UUID ownerUserId) {
        Workspace workspace = authorizationService.requireOwner(workspaceId, ownerUserId);
        AppUser owner = appUserRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", ownerUserId));
        String normalizedEmail = normalizeEmail(invitedEmail);
        if (owner.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("O owner já possui acesso ao workspace.");
        }

        appUserRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (memberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, user.getId())) {
                throw new IllegalStateException("O usuário já é administrador deste workspace.");
            }
        });

        String rawToken = newToken();
        WorkspaceAdminInvitation invitation = invitationRepository
                .findByWorkspace_IdAndInvitedEmailIgnoreCaseAndStatus(
                        workspaceId, normalizedEmail, InvitationStatus.PENDING)
                .orElseGet(WorkspaceAdminInvitation::new);
        invitation.setWorkspace(workspace);
        invitation.setInvitedEmail(normalizedEmail);
        invitation.setTokenHash(hash(rawToken));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS));
        invitation.setInvitedBy(owner);
        invitation.setAcceptedBy(null);
        invitation.setAcceptedAt(null);
        invitation = invitationRepository.save(invitation);

        return toDTO(invitation, rawToken);
    }

    @Transactional
    public WorkspaceMemberDTO accept(@NonNull String rawToken, @NonNull UUID userId) {
        WorkspaceAdminInvitation invitation = invitationRepository
                .findByTokenHashAndStatus(hash(rawToken), InvitationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Convite inválido ou já utilizado"));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new ForbiddenException("O convite expirou.");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        if (!invitation.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ForbiddenException("Este convite foi emitido para outro e-mail.");
        }

        UUID workspaceId = invitation.getWorkspace().getId();
        if (memberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, userId)) {
            throw new IllegalStateException("O usuário já possui acesso a este workspace.");
        }

        WorkspaceMember member = memberRepository.save(
                new WorkspaceMember(invitation.getWorkspace(), user, WorkspaceRole.ADMIN));
        invitation.setAcceptedBy(user);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
        return toMemberDTO(member);
    }

    @Transactional
    public void removeAdmin(@NonNull UUID workspaceId, @NonNull UUID adminUserId, @NonNull UUID ownerUserId) {
        authorizationService.requireOwner(workspaceId, ownerUserId);
        WorkspaceMember member = memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador", adminUserId));
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalStateException("Não é possível remover o owner do workspace.");
        }
        memberRepository.delete(member);
    }

    private WorkspaceAdminInvitationDTO toDTO(WorkspaceAdminInvitation invitation, String rawToken) {
        return new WorkspaceAdminInvitationDTO(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getInvitedEmail(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                rawToken
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

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail do administrador é obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token do convite é obrigatório.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
