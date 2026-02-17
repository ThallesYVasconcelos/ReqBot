package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RequirementDTO {
    private UUID uuid;
    private String requirementId;
    private String rawRequirement;
    private String refinedRequirement;
    private String analise;
    private List<String> ambiguityWarnings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID requirementSetId;
    private String requirementSetName;

    public RequirementDTO() {
    }

    public RequirementDTO(UUID uuid, String requirementId, String rawRequirement, String refinedRequirement,
                         String analise, List<String> ambiguityWarnings, LocalDateTime createdAt,
                         LocalDateTime updatedAt, UUID requirementSetId, String requirementSetName) {
        this.uuid = uuid;
        this.requirementId = requirementId;
        this.rawRequirement = rawRequirement;
        this.refinedRequirement = refinedRequirement;
        this.analise = analise;
        this.ambiguityWarnings = ambiguityWarnings;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.requirementSetId = requirementSetId;
        this.requirementSetName = requirementSetName;
    }

}
