package requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatbotEnrollmentDTO(
        UUID chatbotId,
        String chatbotName,
        UUID projectId,
        String projectName,
        Boolean active,
        Boolean availableNow,
        LocalDateTime joinedAt
) {}
