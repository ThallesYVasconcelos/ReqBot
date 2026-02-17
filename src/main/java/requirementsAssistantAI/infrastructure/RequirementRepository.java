package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    List<Requirement> findByRequirementSet_Id(UUID requirementSetId);
}

