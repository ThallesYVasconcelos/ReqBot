package requirementsAssistantAI.requirementsAssistantAI.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "chatbot_config")
public class ChatbotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_set_id", nullable = false)
    private RequirementSet requirementSet;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ChatbotConfig() {
    }

    public ChatbotConfig(RequirementSet requirementSet, LocalTime startTime, LocalTime endTime) {
        this.requirementSet = requirementSet;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public RequirementSet getRequirementSet() { return requirementSet; }
    public void setRequirementSet(RequirementSet requirementSet) { this.requirementSet = requirementSet; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAvailableNow() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        LocalTime now = LocalTime.now();
        if (startTime == null || endTime == null) {
            return true;
        }
        if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
            return !now.isBefore(startTime) && !now.isAfter(endTime);
        } else {
            return !now.isBefore(startTime) || !now.isAfter(endTime);
        }
    }
}
