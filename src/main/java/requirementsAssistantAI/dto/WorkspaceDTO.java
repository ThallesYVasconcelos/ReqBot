package requirementsAssistantAI.dto;

import requirementsAssistantAI.domain.WorkspaceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WorkspaceDTO(
        UUID id,
        String name,
        String description,
        WorkspaceType type,
        String ownerEmail,
        String inviteCode,
        List<WorkspaceMemberDTO> members,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
