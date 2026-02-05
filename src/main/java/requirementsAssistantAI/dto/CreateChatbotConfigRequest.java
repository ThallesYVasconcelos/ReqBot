package requirementsAssistantAI.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import requirementsAssistantAI.infrastructure.config.JacksonConfig;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class CreateChatbotConfigRequest {
    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;
    
    @JsonDeserialize(using = JacksonConfig.LocalTimeDeserializer.class)
    private LocalTime startTime;
    
    @JsonDeserialize(using = JacksonConfig.LocalTimeDeserializer.class)
    private LocalTime endTime;
}
