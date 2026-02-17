package requirementsAssistantAI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SaveRequirementRequest {

    @NotNull(message = "O ID do RequirementSet é obrigatório")
    private UUID requirementSetId;

    @NotBlank(message = "O prompt/requisito original não pode ser vazio")
    private String rawRequirement;

    @NotBlank(message = "O requisito refinado não pode ser vazio")
    private String refinedRequirement;

    private boolean useRefinedVersion = true;

    private String analise;

    private List<String> ambiguityWarnings;

    public SaveRequirementRequest() {
    }

    public SaveRequirementRequest(UUID requirementSetId, String rawRequirement, String refinedRequirement, boolean useRefinedVersion) {
        this.requirementSetId = requirementSetId;
        this.rawRequirement = rawRequirement;
        this.refinedRequirement = refinedRequirement;
        this.useRefinedVersion = useRefinedVersion;
    }
}
