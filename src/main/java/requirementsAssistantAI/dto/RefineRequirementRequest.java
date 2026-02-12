package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RefineRequirementRequest {

    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;

    @NotBlank(message = "O texto do requisito não pode ser vazio")
    private String requirement;

    public RefineRequirementRequest() {
    }

    public RefineRequirementRequest(UUID requirementSetId, String requirement) {
        this.requirementSetId = requirementSetId;
        this.requirement = requirement;
    }
}
