package requirementsAssistantAI.domain;

import jakarta.persistence.*;
import requirementsAssistantAI.domain.Workspace;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(
        name = "chatbot_config",
        indexes = {
                @Index(name = "idx_chatbot_config_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_chatbot_config_requirement_set_id", columnList = "requirement_set_id"),
                @Index(name = "idx_chatbot_config_workspace_active", columnList = "workspace_id,is_active")
        })
public class ChatbotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "access_code_hash", nullable = false, unique = true, length = 64)
    private String accessCodeHash;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_set_id", nullable = false)
    private RequirementSet requirementSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "show_requirements_to_users", nullable = false)
    private Boolean showRequirementsToUsers = false;

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

    public ChatbotConfig(String name, String accessCodeHash, RequirementSet requirementSet,
                         LocalTime startTime, LocalTime endTime) {
        this.name = name;
        this.accessCodeHash = accessCodeHash;
        this.requirementSet = requirementSet;
        this.workspace = requirementSet != null ? requirementSet.getWorkspace() : null;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
    }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccessCodeHash() { return accessCodeHash; }
    public void setAccessCodeHash(String accessCodeHash) { this.accessCodeHash = accessCodeHash; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public RequirementSet getRequirementSet() { return requirementSet; }
    public void setRequirementSet(RequirementSet requirementSet) { this.requirementSet = requirementSet; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getShowRequirementsToUsers() { return showRequirementsToUsers != null ? showRequirementsToUsers : false; }
    public void setShowRequirementsToUsers(Boolean showRequirementsToUsers) { this.showRequirementsToUsers = showRequirementsToUsers; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAvailableNow() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        LocalTime now = LocalTime.now(ZoneId.of("America/Sao_Paulo"));
        if (startTime == null || endTime == null) {
            return true;
        }
        
        boolean isAvailable;
        
        // Horário normal (ex: 08:00 às 18:00)
        if (startTime.isBefore(endTime)) {
            // Disponível se: now >= startTime && now < endTime
            isAvailable = (now.isAfter(startTime) || now.equals(startTime)) && 
                         now.isBefore(endTime);
        } 
        // Horário que cruza meia-noite (ex: 23:00 às 02:00)
        else if (startTime.isAfter(endTime)) {
            // Disponível se: now >= startTime OU now < endTime
            isAvailable = (now.isAfter(startTime) || now.equals(startTime)) || 
                         now.isBefore(endTime);
        }
        else {
            isAvailable = now.equals(startTime);
        }
        return isAvailable;
    }
}
