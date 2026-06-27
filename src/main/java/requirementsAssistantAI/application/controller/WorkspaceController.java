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

    public WorkspaceController(WorkspaceService workspaceService,
                               RequirementSetService requirementSetService,
                               ChatbotConfigService chatbotConfigService) {
        this.workspaceService = workspaceService;
        this.requirementSetService = requirementSetService;
        this.chatbotConfigService = chatbotConfigService;
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

    @Operation(summary = "Criar configuração de chatbot para um projeto do workspace")
    @PostMapping("/{id}/chatbot/config")
    public ResponseEntity<ChatbotConfigDTO> createChatbotConfig(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChatbotConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        workspaceService.assertAdminOrOwnerById(id, userId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatbotConfigService.createOrUpdateConfig(request, id));
    }

    @Operation(summary = "Configuração ativa do chatbot no workspace")
    @GetMapping("/{id}/chatbot/config/active")
    public ResponseEntity<ChatbotConfigDTO> getActiveChatbotConfig(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        workspaceService.assertMemberById(id, userId(jwt));
        return ResponseEntity.ok(chatbotConfigService.getActiveConfigByWorkspace(id));
    }

    @Operation(summary = "Listar configurações de chatbot do workspace")
    @GetMapping("/{id}/chatbot/config")
    public ResponseEntity<List<ChatbotConfigDTO>> listChatbotConfigs(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        workspaceService.assertAdminOrOwnerById(id, userId(jwt));
        return ResponseEntity.ok(chatbotConfigService.getConfigsByWorkspace(id));
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
