package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT w FROM Workspace w LEFT JOIN FETCH w.members WHERE w.id = :id")
    Optional<Workspace> findByIdWithMembers(@Param("id") UUID id);

    @Query("SELECT DISTINCT w FROM Workspace w LEFT JOIN w.members m " +
           "WHERE w.ownerEmail = :email OR m.userEmail = :email " +
           "ORDER BY w.createdAt DESC")
    List<Workspace> findAllAccessibleByEmail(@Param("email") String email);

    List<Workspace> findByOwnerEmail(String ownerEmail);

    java.util.Optional<Workspace> findByInviteCode(String inviteCode);
}
