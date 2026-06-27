package requirementsAssistantAI.dto;

import requirementsAssistantAI.domain.InvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceAdminInvitationDTO(
        UUID id,
        UUID workspaceId,
        String invitedEmail,
        InvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        String token
) {}
