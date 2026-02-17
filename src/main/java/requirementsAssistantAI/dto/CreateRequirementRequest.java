package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateRequirementRequest {
    
    @NotBlank(message = "O texto do requisito não pode ser vazio")
    private String requirement;
    
    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;


    public CreateRequirementRequest() {
    }

    public CreateRequirementRequest(String requirement, UUID requirementSetId) {
        this.requirement = requirement;
        this.requirementSetId = requirementSetId;
    }


}
