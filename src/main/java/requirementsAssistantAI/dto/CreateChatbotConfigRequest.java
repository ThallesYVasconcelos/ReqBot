package requirementsAssistantAI.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import requirementsAssistantAI.infrastructure.config.JacksonConfig;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class CreateChatbotConfigRequest {
    @NotBlank(message = "O nome do chatbot é obrigatório")
    @Size(max = 120, message = "O nome do chatbot deve ter no máximo 120 caracteres")
    private String name;

    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;
    
    @JsonDeserialize(using = JacksonConfig.LocalTimeDeserializer.class)
    private LocalTime startTime;
    
    @JsonDeserialize(using = JacksonConfig.LocalTimeDeserializer.class)
    private LocalTime endTime;

    private Boolean showRequirementsToUsers = false;
}
