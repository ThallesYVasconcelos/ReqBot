package requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectUserDTO(
        UUID userId,
        String email,
        String name,
        UUID joinedViaChatbotId,
        String joinedViaChatbotName,
        LocalDateTime joinedAt
) {}
