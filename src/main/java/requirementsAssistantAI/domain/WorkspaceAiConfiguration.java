package requirementsAssistantAI.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "workspace_ai_configurations")
public class WorkspaceAiConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProvider provider;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiKey;

    @Column(name = "encryption_iv", nullable = false, length = 32)
    private String encryptionIv;

    @Column(name = "key_hint", nullable = false, length = 16)
    private String keyHint;

    @Column(name = "key_version", nullable = false, length = 20)
    private String keyVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
