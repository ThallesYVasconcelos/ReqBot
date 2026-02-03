package requirementsAssistantAI.requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = "idToken é obrigatório")
    private String idToken;
}
