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
        name = "chatbot_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "chatbot_enrollments_chatbot_user_key",
                columnNames = {"chatbot_id", "user_id"}),
        indexes = {
                @Index(name = "idx_chatbot_enrollments_user_id", columnList = "user_id"),
                @Index(name = "idx_chatbot_enrollments_chatbot_id", columnList = "chatbot_id")
        })
public class ChatbotEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id", nullable = false)
    private ChatbotConfig chatbot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public ChatbotEnrollment() {}

    public ChatbotEnrollment(ChatbotConfig chatbot, AppUser user) {
        this.chatbot = chatbot;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
