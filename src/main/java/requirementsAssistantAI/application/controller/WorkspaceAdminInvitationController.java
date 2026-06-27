package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.application.service.WorkspaceAdminInvitationService;
import requirementsAssistantAI.dto.AcceptAdminInvitationRequest;
import requirementsAssistantAI.dto.CreateAdminInvitationRequest;
import requirementsAssistantAI.dto.WorkspaceAdminInvitationDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;

import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceAdminInvitationController {

    private final WorkspaceAdminInvitationService invitationService;

    public WorkspaceAdminInvitationController(WorkspaceAdminInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Operation(summary = "Convidar ADMIN para o workspace; somente OWNER")
    @PostMapping("/api/workspaces/{workspaceId}/admin-invitations")
    public ResponseEntity<WorkspaceAdminInvitationDTO> invite(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateAdminInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.invite(workspaceId, request.email(), userId(jwt)));
    }

    @Operation(summary = "Aceitar convite de ADMIN")
    @PostMapping("/api/workspace-admin-invitations/accept")
    public ResponseEntity<WorkspaceMemberDTO> accept(
            @Valid @RequestBody AcceptAdminInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(invitationService.accept(request.token(), userId(jwt)));
    }

    @Operation(summary = "Remover ADMIN do workspace; somente OWNER")
    @DeleteMapping("/api/workspaces/{workspaceId}/admins/{adminUserId}")
    public ResponseEntity<Void> removeAdmin(
            @PathVariable UUID workspaceId,
            @PathVariable UUID adminUserId,
            @AuthenticationPrincipal Jwt jwt) {
        invitationService.removeAdmin(workspaceId, adminUserId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
