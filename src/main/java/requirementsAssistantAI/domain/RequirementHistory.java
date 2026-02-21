package requirementsAssistantAI.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import requirementsAssistantAI.infrastructure.config.StringListConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "requirement_history")
public class RequirementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_uuid", nullable = false)
    private Requirement requirement;

    @Column(name = "requirement_id")
    private String requirementId;

    @Column(name = "raw_requirement", columnDefinition = "TEXT")
    private String rawRequirement;

    @Column(name = "refined_requirement", columnDefinition = "TEXT")
    private String refinedRequirement;

    @Column(name = "requirement_hash")
    private String requirementHash;

    @Column(name = "analise", columnDefinition = "TEXT")
    private String analise;

    @Convert(converter = StringListConverter.class)
    @Column(name = "ambiguity_warnings", columnDefinition = "TEXT")
    private List<String> ambiguityWarnings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "action_type")
    private String actionType;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public RequirementHistory() {
    }

    public RequirementHistory(Requirement requirement, String actionType) {
        this.requirement = requirement;
        this.requirementId = requirement.getRequirementId();
        this.rawRequirement = requirement.getRawRequirement();
        this.refinedRequirement = requirement.getRefinedRequirement();
        this.requirementHash = requirement.getRequirementHash();
        this.analise = requirement.getAnalise();
        this.ambiguityWarnings = requirement.getAmbiguityWarnings();
        this.actionType = actionType;
    }
}

