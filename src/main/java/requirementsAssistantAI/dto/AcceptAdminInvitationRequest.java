package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptAdminInvitationRequest(
        @NotBlank(message = "Token do convite é obrigatório")
        String token
) {}
