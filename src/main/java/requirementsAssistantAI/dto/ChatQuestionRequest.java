package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatQuestionRequest {
    @NotBlank(message = "A pergunta não pode ser vazia")
    private String question;
}
