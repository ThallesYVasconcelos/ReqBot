package requirementsAssistantAI.infrastructure;

import requirementsAssistantAI.domain.WorkspaceMember;
import requirementsAssistantAI.domain.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findByWorkspace_Id(UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    boolean existsByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    boolean existsByUser_IdAndRole(UUID userId, WorkspaceRole role);

    void deleteByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);
}
