package requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageDTO(
        UUID id,
        String userEmail,
        String question,
        String answer,
        Boolean answeredFromCache,
        Boolean chatbotAvailable,
        LocalDateTime askedAt,
        UUID requirementSetId,
        String requirementSetName,
        UUID workspaceId,
        String workspaceName
) {}
