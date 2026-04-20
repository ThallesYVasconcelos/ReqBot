package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findByWorkspace_Id(UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspace_IdAndUserEmail(UUID workspaceId, String userEmail);

    boolean existsByWorkspace_IdAndUserEmail(UUID workspaceId, String userEmail);

    void deleteByWorkspace_IdAndUserEmail(UUID workspaceId, String userEmail);
}
