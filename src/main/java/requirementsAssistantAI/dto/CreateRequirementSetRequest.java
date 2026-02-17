package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateRequirementSetRequest {
    
    @NotBlank(message = "O nome do RequirementSet não pode ser vazio")
    @Size(min = 1, max = 255, message = "O nome deve ter entre 1 e 255 caracteres")
    private String name;
    @NotBlank(message = "A descrição do RequirementSet não pode ser vazia")
    @Size(min = 1, max = 2000, message = "A descrição deve ter entre 1 e 2000 caracteres")
    private String description;

    public CreateRequirementSetRequest() {
    }

    public CreateRequirementSetRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {this.description = description;}
}
