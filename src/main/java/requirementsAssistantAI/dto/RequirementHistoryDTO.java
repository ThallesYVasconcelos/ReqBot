package requirementsAssistantAI.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequirementHistoryDTO {

    private UUID id;
    private UUID requirementUuid;
    private String requirementId;
    private String rawRequirement;
    private String refinedRequirement;
    private String analise;
    private List<String> ambiguityWarnings;
    private String actionType;
    private LocalDateTime createdAt;
}
