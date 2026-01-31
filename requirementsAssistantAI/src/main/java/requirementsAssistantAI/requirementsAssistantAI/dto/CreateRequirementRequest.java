package requirementsAssistantAI.requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import opennlp.tools.cmdline.ArgumentParser.OptionalParameter;

import java.util.UUID;

@Getter
@Setter
public class CreateRequirementRequest {
    
    @NotBlank(message = "O texto do requisito não pode ser vazio")
    private String requirement;
    
    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;

    @OptionalParameter()
    private String description;

    public CreateRequirementRequest() {
    }

    public CreateRequirementRequest(String requirement,String description, UUID requirementSetId) {
        this.requirement = requirement;
        this.requirementSetId = requirementSetId;
        this.description = description;
    }


}
