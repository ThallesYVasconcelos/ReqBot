package requirementsAssistantAI.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "project_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "project_enrollments_project_user_key",
                columnNames = {"requirement_set_id", "user_id"}),
        indexes = {
                @Index(name = "idx_project_enrollments_requirement_set_id", columnList = "requirement_set_id"),
                @Index(name = "idx_project_enrollments_user_id", columnList = "user_id"),
                @Index(name = "idx_project_enrollments_joined_via_chatbot_id", columnList = "joined_via_chatbot_id")
        })
public class ProjectEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_set_id", nullable = false)
    private RequirementSet project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "joined_via_chatbot_id")
    private ChatbotConfig joinedViaChatbot;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public ProjectEnrollment() {}

    public ProjectEnrollment(RequirementSet project, AppUser user, ChatbotConfig joinedViaChatbot) {
        this.project = project;
        this.user = user;
        this.joinedViaChatbot = joinedViaChatbot;
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
