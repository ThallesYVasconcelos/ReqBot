package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import requirementsAssistantAI.domain.AiProvider;

public record UpdateWorkspaceAiConfigurationRequest(
        @NotNull AiProvider provider,
        @NotBlank @Size(max = 120) String model,
        @NotBlank @Size(max = 1024) String apiKey) {
}
