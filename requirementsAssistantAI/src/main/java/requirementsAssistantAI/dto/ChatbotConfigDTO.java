package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class ChatbotConfigDTO {
    private UUID id;
    private Boolean isActive;
    private UUID requirementSetId;
    private String requirementSetName;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean availableNow;

    public ChatbotConfigDTO() {
    }

    public ChatbotConfigDTO(UUID id, Boolean isActive, UUID requirementSetId, String requirementSetName,
                           LocalTime startTime, LocalTime endTime, LocalDateTime createdAt, LocalDateTime updatedAt,
                           Boolean availableNow) {
        this.id = id;
        this.isActive = isActive;
        this.requirementSetId = requirementSetId;
        this.requirementSetName = requirementSetName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.availableNow = availableNow;
    }
}
