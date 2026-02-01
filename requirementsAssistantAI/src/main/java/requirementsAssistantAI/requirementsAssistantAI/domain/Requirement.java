package com.tcc.requirements_assistant_api.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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
    
    @Column(name = "analise", columnDefinition = "TEXT")
    private String analise;

    @Column(name = "status", nullable = false)
    private String status;

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
        if (status == null) {
            status = "PENDING";
        }
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

