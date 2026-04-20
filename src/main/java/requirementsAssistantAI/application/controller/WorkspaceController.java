package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import requirementsAssistantAI.application.service.WorkspaceService;
import requirementsAssistantAI.dto.AddMemberRequest;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "Criar workspace (PROFESSIONAL ou ACADEMIC)")
    @PostMapping
    public ResponseEntity<WorkspaceDTO> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        WorkspaceDTO dto = workspaceService.createWorkspace(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Listar workspaces acessíveis pelo usuário autenticado")
    @GetMapping
    public ResponseEntity<List<WorkspaceDTO>> listMine(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(workspaceService.getMyWorkspaces(email));
    }

    @Operation(summary = "Buscar workspace por ID")
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceDTO> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(workspaceService.getById(id, email));
    }

    @Operation(summary = "Atualizar workspace")
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, request, email));
    }

    @Operation(summary = "Excluir workspace (somente o dono)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        workspaceService.deleteWorkspace(id, email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Adicionar membro ao workspace")
    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMemberDTO> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        WorkspaceMemberDTO dto = workspaceService.addMember(id, request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Remover membro do workspace")
    @DeleteMapping("/{id}/members/{memberEmail}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable String memberEmail,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        workspaceService.removeMember(id, memberEmail, email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Histórico de perguntas do chat neste workspace")
    @GetMapping("/{id}/chat-history")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(workspaceService.getChatHistory(id, email));
    }
}
