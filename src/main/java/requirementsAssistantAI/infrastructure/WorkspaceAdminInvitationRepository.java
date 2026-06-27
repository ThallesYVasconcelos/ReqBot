package requirementsAssistantAI.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import requirementsAssistantAI.domain.InvitationStatus;
import requirementsAssistantAI.domain.WorkspaceAdminInvitation;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceAdminInvitationRepository extends JpaRepository<WorkspaceAdminInvitation, UUID> {

    Optional<WorkspaceAdminInvitation> findByTokenHashAndStatus(String tokenHash, InvitationStatus status);

    Optional<WorkspaceAdminInvitation> findByWorkspace_IdAndInvitedEmailIgnoreCaseAndStatus(
            UUID workspaceId,
            String invitedEmail,
            InvitationStatus status
    );
}
