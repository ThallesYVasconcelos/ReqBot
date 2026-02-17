package requirementsAssistantAI.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import requirementsAssistantAI.infrastructure.config.StringListConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "requirements")
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "requirement_id")
    private String requirementId;

    @Column(name = "refined_requirement", columnDefinition = "TEXT")
    private String refinedRequirement;

    @Column(name="requirement_hash")
    private String requirementHash;

    @Column(name = "raw_requirement", columnDefinition = "TEXT")
    private String rawRequirement;
    
    @Column(name = "analise", columnDefinition = "TEXT")
    private String analise;

    @Convert(converter = StringListConverter.class)
    @Column(name = "ambiguity_warnings", columnDefinition = "TEXT")
    private List<String> ambiguityWarnings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_set_id", nullable = false)
    @JsonIgnore
    private RequirementSet requirementSet;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Requirement() {
    }

    public Requirement(String requirementId, RequirementSet requirementSet) {
        this.requirementId = requirementId;
        this.requirementSet = requirementSet;
    }

   


}

