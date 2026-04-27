package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.RequirementSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RequirementSetRepository extends JpaRepository<RequirementSet, UUID> {

    java.util.List<RequirementSet> findByWorkspace_Id(UUID workspaceId);
}

