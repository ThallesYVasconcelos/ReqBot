package requirementsAssistantAI.requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.requirementsAssistantAI.domain.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    Optional<Requirement> findByRequirementId(String requirementId);
    List<Requirement> findByRequirementSet_Id(UUID requirementSetId);
    boolean existsByRequirementHashAndStatus(String requirementHash, String status);
    List<Requirement> findByRequirementSet_IdAndStatus(UUID requirementSetId, String status);
}

