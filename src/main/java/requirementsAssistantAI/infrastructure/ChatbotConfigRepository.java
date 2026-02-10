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
    Optional<ChatbotConfig> findByIsActiveTrue();
    
    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet WHERE c.isActive = true")
    Optional<ChatbotConfig> findByIsActiveTrueWithRequirementSet();
    
    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet")
    List<ChatbotConfig> findAllWithRequirementSet();
    
    @Query("SELECT c FROM ChatbotConfig c LEFT JOIN FETCH c.requirementSet WHERE c.id = :id")
    Optional<ChatbotConfig> findByIdWithRequirementSet(@Param("id") UUID id);

    boolean existsByRequirementSet_Id(UUID requirementSetId);

    List<ChatbotConfig> findByRequirementSet_Id(UUID requirementSetId);
}
