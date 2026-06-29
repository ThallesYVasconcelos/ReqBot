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

    @Query(value = """
            SELECT r.*
            FROM requirements r
            WHERE r.requirement_set_id = :projectId
              AND to_tsvector(
                    'simple',
                    coalesce(r.requirement_id, '') || ' ' ||
                    coalesce(r.refined_requirement, '') || ' ' ||
                    coalesce(r.raw_requirement, '')
                  ) @@ websearch_to_tsquery('simple', :query)
            ORDER BY ts_rank_cd(
                    to_tsvector(
                      'simple',
                      coalesce(r.requirement_id, '') || ' ' ||
                      coalesce(r.refined_requirement, '') || ' ' ||
                      coalesce(r.raw_requirement, '')
                    ),
                    websearch_to_tsquery('simple', :query)
                  ) DESC,
                  r.updated_at DESC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<Requirement> searchLiteral(
            @Param("projectId") UUID projectId,
            @Param("query") String query,
            @Param("limit") int limit);

    @Query("SELECT r FROM Requirement r JOIN FETCH r.requirementSet WHERE r.uuid = :id")
    Optional<Requirement> findByIdWithRequirementSet(@Param("id") UUID id);
}

