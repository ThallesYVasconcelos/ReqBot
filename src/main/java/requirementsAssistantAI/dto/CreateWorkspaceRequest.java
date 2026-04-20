package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import requirementsAssistantAI.domain.WorkspaceType;

public record CreateWorkspaceRequest(

        @NotBlank(message = "Nome do workspace é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String description,

        @NotNull(message = "Tipo do workspace é obrigatório: PROFESSIONAL ou ACADEMIC")
        WorkspaceType type
) {}
