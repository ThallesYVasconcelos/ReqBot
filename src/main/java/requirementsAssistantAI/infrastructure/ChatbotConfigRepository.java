package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.ChatbotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotConfigRepository extends JpaRepository<ChatbotConfig, UUID> {
    Optional<ChatbotConfig> findFirstByIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT c FROM ChatbotConfig c JOIN FETCH c.requirementSet LEFT JOIN FETCH c.workspace WHERE c.id = :id")
    Optional<ChatbotConfig> findByIdWithProjectAndWorkspace(@Param("id") UUID id);

    Optional<ChatbotConfig> findByAccessCodeHashAndIsActiveTrue(String accessCodeHash);

    boolean existsByAccessCodeHash(String accessCodeHash);

    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet")
    List<ChatbotConfig> findAllWithRequirementSet();

    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet WHERE c.workspace.id = :workspaceId ORDER BY c.createdAt DESC")
    List<ChatbotConfig> findAllByWorkspace_Id(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet WHERE c.requirementSet.id = :projectId ORDER BY c.createdAt DESC")
    List<ChatbotConfig> findAllByRequirementSet_Id(@Param("projectId") UUID projectId);

    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet WHERE c.id = :id")
    Optional<ChatbotConfig> findByIdWithRequirementSet(@Param("id") UUID id);

    boolean existsByRequirementSet_Id(UUID requirementSetId);

    List<ChatbotConfig> findByRequirementSet_Id(UUID requirementSetId);
}
