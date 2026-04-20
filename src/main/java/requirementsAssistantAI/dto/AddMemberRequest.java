package requirementsAssistantAI.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import requirementsAssistantAI.domain.WorkspaceRole;

public record AddMemberRequest(

        @NotBlank(message = "E-mail do membro é obrigatório")
        @Email(message = "E-mail inválido")
        String userEmail,

        @NotNull(message = "Role é obrigatório: OWNER, ADMIN ou MEMBER")
        WorkspaceRole role
) {}
