package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {

    List<Requirement> findByRequirementSet_Id(UUID requirementSetId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.requirementId, 5) AS int)), 0) " +
           "FROM Requirement r WHERE r.requirementSet.id = :setId " +
           "AND r.requirementId LIKE 'REQ-%'")
    Optional<Integer> findMaxRequirementNumber(@Param("setId") UUID setId);

    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.requirementSet.id = :setId")
    long countByRequirementSetId(@Param("setId") UUID setId);
}

