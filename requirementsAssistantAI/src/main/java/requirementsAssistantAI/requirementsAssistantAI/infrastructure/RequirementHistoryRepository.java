package requirementsAssistantAI.requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.requirementsAssistantAI.domain.RequirementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementHistoryRepository extends JpaRepository<RequirementHistory, UUID> {
    List<RequirementHistory> findByRequirement_UuidOrderByCreatedAtDesc(UUID requirementUuid);
}

