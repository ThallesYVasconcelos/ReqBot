package requirementsAssistantAI.requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.requirementsAssistantAI.domain.RequirementSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RequirementSetRepository extends JpaRepository<RequirementSet, UUID> {
}

