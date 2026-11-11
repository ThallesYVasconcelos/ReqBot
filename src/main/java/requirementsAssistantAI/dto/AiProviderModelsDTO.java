package requirementsAssistantAI.dto;

import requirementsAssistantAI.domain.AiProvider;

import java.util.List;

public record AiProviderModelsDTO(AiProvider provider, List<String> models) {
}
