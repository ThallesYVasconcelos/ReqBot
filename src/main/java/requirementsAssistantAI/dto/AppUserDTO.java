package requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AppUserDTO {
    private UUID id;
    private String email;
    private String name;
    private String pictureUrl;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AppUserDTO() {}

    public AppUserDTO(UUID id, String email, String name, String pictureUrl, String role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
