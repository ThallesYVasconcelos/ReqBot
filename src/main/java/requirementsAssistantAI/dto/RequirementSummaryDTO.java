package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class RequirementSummaryDTO {
    private UUID uuid;
    private String requirementId;
    private String refinedRequirement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    public RequirementSummaryDTO() {
    }

    public RequirementSummaryDTO(UUID uuid, String requirementId, String refinedRequirement, String description,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.uuid = uuid;
        this.requirementId = requirementId;
        this.refinedRequirement = refinedRequirement;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.description = description;
    }

    
}
