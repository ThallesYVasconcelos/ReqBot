package requirementsAssistantAI.dto;

import requirementsAssistantAI.domain.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceMemberDTO(
        UUID id,
        String userEmail,
        WorkspaceRole role,
        LocalDateTime createdAt
) {}
