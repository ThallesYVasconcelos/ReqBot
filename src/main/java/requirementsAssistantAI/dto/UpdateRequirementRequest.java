package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRequirementRequest {

    @NotBlank(message = "O texto do requisito não pode ser vazio")
    private String requirement;

    public UpdateRequirementRequest() {
    }

    public UpdateRequirementRequest(String requirement) {
        this.requirement = requirement;
    }
}
