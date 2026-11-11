package requirementsAssistantAI.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.application.service.WorkspaceAiService;
import requirementsAssistantAI.dto.AiProviderModelsDTO;
import requirementsAssistantAI.dto.UpdateWorkspaceAiConfigurationRequest;
import requirementsAssistantAI.dto.WorkspaceAiConfigurationDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/ai-configuration")
public class WorkspaceAiConfigurationController {
    private final WorkspaceAiService workspaceAiService;

    public WorkspaceAiConfigurationController(WorkspaceAiService workspaceAiService) {
        this.workspaceAiService = workspaceAiService;
    }

    @GetMapping
    public ResponseEntity<WorkspaceAiConfigurationDTO> get(
            @PathVariable UUID workspaceId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workspaceAiService.getConfiguration(workspaceId, userId(jwt)));
    }

    @GetMapping("/models")
    public ResponseEntity<List<AiProviderModelsDTO>> models(
            @PathVariable UUID workspaceId, @AuthenticationPrincipal Jwt jwt) {
        // Também confirma que apenas o owner tenha acesso às opções de configuração.
        workspaceAiService.getConfiguration(workspaceId, userId(jwt));
        return ResponseEntity.ok(workspaceAiService.availableModels());
    }

    @PutMapping
    public ResponseEntity<WorkspaceAiConfigurationDTO> save(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceAiConfigurationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workspaceAiService.saveConfiguration(workspaceId, userId(jwt), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID workspaceId, @AuthenticationPrincipal Jwt jwt) {
        workspaceAiService.deleteConfiguration(workspaceId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
