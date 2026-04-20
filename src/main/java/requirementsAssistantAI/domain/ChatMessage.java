package requirementsAssistantAI.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra cada interação do usuário com o chatbot.
 * Vinculada ao projeto (RequirementSet) ativo no momento da pergunta.
 */
@Getter
@Setter
@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_chat_messages_requirement_set_id", columnList = "requirement_set_id"),
        @Index(name = "idx_chat_messages_user_email", columnList = "user_email"),
        @Index(name = "idx_chat_messages_asked_at", columnList = "asked_at")
    }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_from_cache")
    private Boolean answeredFromCache;

    @Column(name = "chatbot_available")
    private Boolean chatbotAvailable;

    @Column(name = "asked_at", nullable = false)
    private LocalDateTime askedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_set_id")
    private RequirementSet requirementSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @PrePersist
    protected void onCreate() {
        if (askedAt == null) {
            askedAt = LocalDateTime.now();
        }
    }

    public ChatMessage() {}

    public ChatMessage(String userEmail, String question, String answer,
                       boolean answeredFromCache, boolean chatbotAvailable,
                       RequirementSet requirementSet, Workspace workspace) {
        this.userEmail = userEmail;
        this.question = question;
        this.answer = answer;
        this.answeredFromCache = answeredFromCache;
        this.chatbotAvailable = chatbotAvailable;
        this.requirementSet = requirementSet;
        this.workspace = workspace;
        this.askedAt = LocalDateTime.now();
    }
}
