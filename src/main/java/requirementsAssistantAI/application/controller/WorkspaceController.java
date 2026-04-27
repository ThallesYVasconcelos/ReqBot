package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import requirementsAssistantAI.application.service.ChatbotConfigService;
import requirementsAssistantAI.application.service.RequirementSetService;
import requirementsAssistantAI.application.service.WorkspaceService;
import requirementsAssistantAI.dto.AddMemberRequest;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.ChatQuestionClusterDTO;
import requirementsAssistantAI.dto.ChatbotConfigDTO;
import requirementsAssistantAI.dto.CreateChatbotConfigRequest;
import requirementsAssistantAI.dto.CreateRequirementSetRequest;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.RequirementSetDTO;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final RequirementSetService requirementSetService;
    private final ChatbotConfigService chatbotConfigService;

    public WorkspaceController(WorkspaceService workspaceService,
                               RequirementSetService requirementSetService,
                               ChatbotConfigService chatbotConfigService) {
        this.workspaceService = workspaceService;
        this.requirementSetService = requirementSetService;
        this.chatbotConfigService = chatbotConfigService;
    }

    // ── Workspace CRUD ──────────────────────────────────────────────────────

    @Operation(summary = "Criar workspace — somente ADMIN")
    @PostMapping
    public ResponseEntity<WorkspaceDTO> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        WorkspaceDTO dto = workspaceService.createWorkspace(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Listar workspaces acessíveis pelo usuário autenticado")
    @GetMapping
    public ResponseEntity<List<WorkspaceDTO>> listMine(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(workspaceService.getMyWorkspaces(email));
    }

    @Operation(summary = "Buscar workspace por ID")
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceDTO> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(workspaceService.getById(id, email));
    }

    @Operation(summary = "Atualizar workspace")
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, request, email));
    }

    @Operation(summary = "Excluir workspace (somente o dono)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        workspaceService.deleteWorkspace(id, email);
        return ResponseEntity.noContent().build();
    }

    // ── Membros ──────────────────────────────────────────────────────────────

    @Operation(summary = "Adicionar membro ao workspace por email")
    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMemberDTO> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        WorkspaceMemberDTO dto = workspaceService.addMember(id, request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Remover membro do workspace")
    @DeleteMapping("/{id}/members/{memberEmail}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable String memberEmail,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        workspaceService.removeMember(id, memberEmail, email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Aluno entra no workspace com código de convite")
    @PostMapping("/join")
    public ResponseEntity<WorkspaceDTO> joinByCode(
            @RequestParam String code,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(workspaceService.joinByInviteCode(code, email));
    }

    // ── Projetos (RequirementSets) por workspace ──────────────────────────────

    @Operation(summary = "Criar projeto (RequirementSet) dentro de um workspace — admin/owner")
    @PostMapping("/{id}/requirement-sets")
    public ResponseEntity<RequirementSetDTO> createProject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRequirementSetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requirementSetService.createRequirementSetInWorkspace(id, request.getName(), request.getDescription(), email));
    }

    @Operation(summary = "Listar projetos do workspace")
    @GetMapping("/{id}/requirement-sets")
    public ResponseEntity<List<RequirementSetDTO>> listProjects(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(requirementSetService.getRequirementSetsByWorkspace(id, email));
    }

    // ── Chatbot config por workspace ─────────────────────────────────────────

    @Operation(summary = "Criar/atualizar config do chatbot no workspace — admin/owner")
    @PostMapping("/{id}/chatbot/config")
    public ResponseEntity<ChatbotConfigDTO> createChatbotConfig(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChatbotConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        workspaceService.assertAdminOrOwnerById(id, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatbotConfigService.createOrUpdateConfig(request));
    }

    @Operation(summary = "Config ativa do chatbot no workspace")
    @GetMapping("/{id}/chatbot/config/active")
    public ResponseEntity<ChatbotConfigDTO> getActiveChatbotConfig(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        workspaceService.assertMemberById(id, email);
        return ResponseEntity.ok(chatbotConfigService.getActiveConfigByWorkspace(id));
    }

    @Operation(summary = "Listar todas as configs do chatbot no workspace — admin/owner")
    @GetMapping("/{id}/chatbot/config")
    public ResponseEntity<List<ChatbotConfigDTO>> listChatbotConfigs(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        workspaceService.assertAdminOrOwnerById(id, email);
        return ResponseEntity.ok(chatbotConfigService.getConfigsByWorkspace(id));
    }

    // ── Histórico e ranking ──────────────────────────────────────────────────

    @Operation(summary = "Histórico de perguntas do chat neste workspace (admin/owner)")
    @GetMapping("/{id}/chat-history")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(workspaceService.getChatHistory(id, email));
    }

    @Operation(summary = "Ranking anônimo de perguntas similares do chat")
    @GetMapping("/{id}/chat-question-ranking")
    public ResponseEntity<List<ChatQuestionClusterDTO>> getChatQuestionRanking(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.82") double similarityThreshold,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(
                workspaceService.getAnonymousQuestionRanking(id, email, limit, similarityThreshold)
        );
    }
}
