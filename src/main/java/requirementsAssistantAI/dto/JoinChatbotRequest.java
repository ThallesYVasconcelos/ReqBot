package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinChatbotRequest(
        @NotBlank(message = "O código do chatbot é obrigatório")
        String code
) {}
