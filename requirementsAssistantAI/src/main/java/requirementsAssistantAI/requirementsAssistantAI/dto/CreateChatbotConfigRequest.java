package requirementsAssistantAI.requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class CreateChatbotConfigRequest {
    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;
    private LocalTime startTime;
    private LocalTime endTime;
}
