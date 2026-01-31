package requirementsAssistantAI.requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.UUID;

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

    // Construtor padrão
    public RequirementDTO() {
    }

    // Construtor completo
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

    // Getters e Setters
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getRefinedRequirement() {
        return refinedRequirement;
    }

    public void setRefinedRequirement(String refinedRequirement) {
        this.refinedRequirement = refinedRequirement;
    }

    public String getAnalise() {
        return analise;
    }

    public void setAnalise(String analise) {
        this.analise = analise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getRequirementSetId() {
        return requirementSetId;
    }

    public void setRequirementSetId(UUID requirementSetId) {
        this.requirementSetId = requirementSetId;
    }

    public String getRequirementSetName() {
        return requirementSetName;
    }

    public void setRequirementSetName(String requirementSetName) {
        this.requirementSetName = requirementSetName;
    }
}
