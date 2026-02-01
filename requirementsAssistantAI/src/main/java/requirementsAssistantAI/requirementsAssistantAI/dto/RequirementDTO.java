package requirementsAssistantAI.requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RequirementDTO {
    private UUID uuid;
    private String requirementId;
    private String refinedRequirement;
    private String analise;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID requirementSetId;
    private String requirementSetName;

    public RequirementDTO() {
    }

    public RequirementDTO(UUID uuid, String requirementId, String refinedRequirement, 
                         String analise, String status, LocalDateTime createdAt, 
                         LocalDateTime updatedAt, UUID requirementSetId, String requirementSetName) {
        this.uuid = uuid;
        this.requirementId = requirementId;
        this.refinedRequirement = refinedRequirement;
        this.analise = analise;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.requirementSetId = requirementSetId;
        this.requirementSetName = requirementSetName;
    }

}
