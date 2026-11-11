package requirementsAssistantAI.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import requirementsAssistantAI.domain.WorkspaceAiConfiguration;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceAiConfigurationRepository extends JpaRepository<WorkspaceAiConfiguration, UUID> {
    Optional<WorkspaceAiConfiguration> findByWorkspace_Id(UUID workspaceId);
}
