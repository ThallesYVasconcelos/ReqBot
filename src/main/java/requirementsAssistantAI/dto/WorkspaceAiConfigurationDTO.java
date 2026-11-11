package requirementsAssistantAI.dto;

import requirementsAssistantAI.domain.AiProvider;

import java.time.LocalDateTime;

public record WorkspaceAiConfigurationDTO(
        boolean configured,
        AiProvider provider,
        String model,
        String keyHint,
        LocalDateTime updatedAt) {
}
