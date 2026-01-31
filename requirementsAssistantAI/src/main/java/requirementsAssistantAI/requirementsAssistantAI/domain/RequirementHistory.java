package com.tcc.requirements_assistant_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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

    @Column(name = "requirement_id", nullable = false)
    private String requirementId;

    @Column(name = "refined_requirement", columnDefinition = "TEXT")
    private String refinedRequirement;

    @Column(name = "analise", columnDefinition = "TEXT")
    private String analise;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "action_type")
    private String actionType; // CREATED, UPDATED, DELETED

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public RequirementHistory() {
    }

    public RequirementHistory(Requirement requirement, String actionType) {
        this.requirement = requirement;
        this.requirementId = requirement.getRequirementId();
        this.refinedRequirement = requirement.getRefinedRequirement();
        this.analise = requirement.getAnalise();
        this.status = requirement.getStatus();
        this.actionType = actionType;
    }

    
}

