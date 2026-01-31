package requirementsAssistantAI.requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateRequirementSetRequest {
    
    @NotBlank(message = "O nome do RequirementSet não pode ser vazio")
    @Size(min = 1, max = 255, message = "O nome deve ter entre 1 e 255 caracteres")
    private String name;

    // Construtor padrão
    public CreateRequirementSetRequest() {
    }

    // Construtor com parâmetro
    public CreateRequirementSetRequest(String name) {
        this.name = name;
    }

    // Getters e Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
