package requirementsAssistantAI.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAdminInvitationRequest(
        @NotBlank(message = "E-mail do administrador é obrigatório")
        @Email(message = "E-mail inválido")
        String email
) {}
