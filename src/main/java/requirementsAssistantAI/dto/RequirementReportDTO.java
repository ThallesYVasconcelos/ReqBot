package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RequirementReportDTO {

    private UUID requirementSetId;
    private String requirementSetName;
    private List<RequirementReportItemDTO> requirementsWithProblems;


    public RequirementReportDTO(UUID requirementSetId, String requirementSetName, List<RequirementReportItemDTO> requirementsWithProblems) {
        this.requirementSetId = requirementSetId;
        this.requirementSetName = requirementSetName;
        this.requirementsWithProblems = requirementsWithProblems;
    }

    @Getter
    @Setter
    public static class RequirementReportItemDTO {
        private UUID requirementId;
        private String requirementIdStr;
        private String refinedRequirement;
        private List<String> problems;
        private List<ConflictInfo> conflicts;
        private List<String> resolutionSuggestions;
        /** Pontos de ambiguidade (quando o problema é ambiguidade, não conflito) */
        private List<String> ambiguityWarnings;




        public RequirementReportItemDTO(UUID requirementId, String requirementIdStr, String refinedRequirement,
                                       List<String> problems, List<ConflictInfo> conflicts, List<String> resolutionSuggestions,
                                       List<String> ambiguityWarnings) {
            this.requirementId = requirementId;
            this.requirementIdStr = requirementIdStr;
            this.refinedRequirement = refinedRequirement;
            this.problems = problems;
            this.conflicts = conflicts;
            this.resolutionSuggestions = resolutionSuggestions;
            this.ambiguityWarnings = ambiguityWarnings;
        }
    }

    @Getter
    @Setter
    public static class ConflictInfo {
        private UUID conflictingRequirementId;
        private String conflictingRequirementIdStr;
        private String conflictingText;
        private double similarityScore;
        private String suggestion;


        public ConflictInfo(UUID conflictingRequirementId, String conflictingRequirementIdStr, String conflictingText,
                           double similarityScore, String suggestion) {
            this.conflictingRequirementId = conflictingRequirementId;
            this.conflictingRequirementIdStr = conflictingRequirementIdStr;
            this.conflictingText = conflictingText;
            this.similarityScore = similarityScore;
            this.suggestion = suggestion;
        }
    }
}
