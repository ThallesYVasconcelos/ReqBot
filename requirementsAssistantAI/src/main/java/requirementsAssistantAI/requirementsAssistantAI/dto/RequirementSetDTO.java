package requirementsAssistantAI.requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class RequirementSetDTO {
    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer requirementsCount;

    // Construtor padrão
    public RequirementSetDTO() {
    }

    // Construtor completo
    public RequirementSetDTO(UUID id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, Integer requirementsCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.requirementsCount = requirementsCount;
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getRequirementsCount() {
        return requirementsCount;
    }

    public void setRequirementsCount(Integer requirementsCount) {
        this.requirementsCount = requirementsCount;
    }
}
