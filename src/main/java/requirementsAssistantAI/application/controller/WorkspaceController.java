package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.application.service.ChatService;
import requirementsAssistantAI.application.service.ChatbotConfigService;
import requirementsAssistantAI.application.service.RequirementSetService;
import requirementsAssistantAI.application.service.WorkspaceService;
import requirementsAssistantAI.dto.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final RequirementSetService requirementSetService;
    private final ChatbotConfigService chatbotConfigService;
    private final ChatService chatService;

    public WorkspaceController(
            WorkspaceService workspaceService,
            RequirementSetService requirementSetService,
            ChatbotConfigService chatbotConfigService,
            ChatService chatService) {
        this.workspaceService = workspaceService;
        this.requirementSetService = requirementSetService;
        this.chatbotConfigService = chatbotConfigService;
        this.chatService = chatService;
    }

    @Operation(summary = "Criar workspace; o usuário autenticado torna-se OWNER")
    @PostMapping
    public ResponseEntity<WorkspaceDTO> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.createWorkspace(request, userId(jwt)));
    }

    @Operation(summary = "Listar workspaces em que o usuário é OWNER ou ADMIN")
    @GetMapping
    public ResponseEntity<List<WorkspaceDTO>> listMine(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workspaceService.getMyWorkspaces(userId(jwt)));
    }

    @Operation(summary = "Buscar workspace por ID")
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceDTO> getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workspaceService.getById(id, userId(jwt)));
    }

    @Operation(summary = "Criar projeto dentro do workspace")
    @PostMapping("/{id}/requirement-sets")
    public ResponseEntity<RequirementSetDTO> createProject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRequirementSetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                requirementSetService.createRequirementSetInWorkspace(
                        id, request.getName(), request.getDescription(), userId(jwt)));
    }

    @Operation(summary = "Listar projetos do workspace")
    @GetMapping("/{id}/requirement-sets")
    public ResponseEntity<List<RequirementSetDTO>> listProjects(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requirementSetService.getRequirementSetsByWorkspace(id, userId(jwt)));
    }

    @Operation(summary = "Criar chatbot para um projeto do workspace")
    @PostMapping("/{id}/chatbots")
    public ResponseEntity<ChatbotConfigDTO> createChatbot(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChatbotConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatbotConfigService.create(request, id, userId(jwt)));
    }

    @Operation(summary = "Listar chatbots do workspace")
    @GetMapping("/{id}/chatbots")
    public ResponseEntity<List<ChatbotConfigDTO>> listChatbots(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatbotConfigService.listByWorkspace(id, userId(jwt)));
    }

    @Operation(summary = "Listar chatbots de um projeto")
    @GetMapping("/{id}/projects/{projectId}/chatbots")
    public ResponseEntity<List<ChatbotConfigDTO>> listProjectChatbots(
            @PathVariable UUID id,
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        workspaceService.assertAdminOrOwnerById(id, userId(jwt));
        return ResponseEntity.ok(chatbotConfigService.listByProject(id, projectId, userId(jwt)));
    }

    @Operation(summary = "Ativar ou desativar chatbot")
    @PatchMapping("/{id}/chatbots/{chatbotId}/active")
    public ResponseEntity<ChatbotConfigDTO> setChatbotActive(
            @PathVariable UUID id,
            @PathVariable UUID chatbotId,
            @RequestParam boolean active,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatbotConfigService.setActive(id, chatbotId, active, userId(jwt)));
    }

    @Operation(summary = "Histórico completo de um chatbot")
    @GetMapping("/{id}/chatbots/{chatbotId}/history")
    public ResponseEntity<List<ChatMessageDTO>> getChatbotHistory(
            @PathVariable UUID id,
            @PathVariable UUID chatbotId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getChatHistoryForManager(chatbotId, id, userId(jwt)));
    }

    @Operation(summary = "Histórico de perguntas do workspace")
    @GetMapping("/{id}/chat-history")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workspaceService.getChatHistory(id, userId(jwt)));
    }

    @Operation(summary = "Ranking anônimo de perguntas similares")
    @GetMapping("/{id}/chat-question-ranking")
    public ResponseEntity<List<ChatQuestionClusterDTO>> getChatQuestionRanking(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.82") double similarityThreshold,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workspaceService.getAnonymousQuestionRanking(
                id, userId(jwt), limit, similarityThreshold));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
