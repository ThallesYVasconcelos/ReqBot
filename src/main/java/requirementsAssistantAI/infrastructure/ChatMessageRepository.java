package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByRequirementSet_IdOrderByAskedAtDesc(UUID requirementSetId);

    List<ChatMessage> findByUserEmailOrderByAskedAtDesc(String userEmail);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.workspace.id = :workspaceId ORDER BY cm.askedAt DESC")
    List<ChatMessage> findByWorkspaceIdOrderByAskedAtDesc(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.requirementSet.id = :setId AND cm.userEmail = :email ORDER BY cm.askedAt DESC")
    List<ChatMessage> findByRequirementSetAndUser(@Param("setId") UUID setId, @Param("email") String email);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.requirementSet.id = :setId")
    long countByRequirementSetId(@Param("setId") UUID setId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.workspace.id = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userEmail = :email AND cm.workspace.id = :workspaceId ORDER BY cm.askedAt DESC")
    List<ChatMessage> findByUserEmailAndWorkspaceId(@Param("email") String email, @Param("workspaceId") UUID workspaceId);
}
