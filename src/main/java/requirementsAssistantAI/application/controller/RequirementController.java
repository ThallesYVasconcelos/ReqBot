package requirementsAssistantAI.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.application.service.RequirementService;
import requirementsAssistantAI.application.service.WorkspaceAuthorizationService;
import requirementsAssistantAI.dto.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final WorkspaceAuthorizationService authorizationService;

    public RequirementController(
            RequirementService requirementService,
            WorkspaceAuthorizationService authorizationService) {
        this.requirementService = requirementService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<RequirementDTO> refineRequirement(
            @Valid @RequestBody CreateRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID projectId = request.getRequirementSetId();
        authorizationService.requireOwnerOrAdminForProject(projectId, userId(jwt));
        return ResponseEntity.ok(requirementService.refineRequirement(request.getRequirement(), projectId));
    }

    @PostMapping("/refine")
    public ResponseEntity<RequirementDTO> refine(
            @Valid @RequestBody RefineRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID projectId = request.getRequirementSetId();
        authorizationService.requireOwnerOrAdminForProject(projectId, userId(jwt));
        return ResponseEntity.ok(requirementService.refineRequirement(request.getRequirement(), projectId));
    }

    @PostMapping("/save")
    public ResponseEntity<RequirementDTO> saveRequirement(
            @Valid @RequestBody SaveRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForProject(request.getRequirementSetId(), userId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(requirementService.saveRequirement(request));
    }

    @GetMapping
    public ResponseEntity<List<RequirementDTO>> getAllRequirements(
            @RequestParam UUID requirementSetId,
            @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForProject(requirementSetId, userId(jwt));
        return ResponseEntity.ok(requirementService.getAllRequirements(requirementSetId));
    }

    @GetMapping("/report")
    public ResponseEntity<RequirementReportDTO> getGeneralReport(
            @RequestParam UUID requirementSetId,
            @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForProject(requirementSetId, userId(jwt));
        return ResponseEntity.ok(requirementService.getGeneralReport(requirementSetId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequirementDTO> getRequirement(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForRequirement(id, userId(jwt));
        return ResponseEntity.ok(requirementService.getRequirementById(id));
    }

    @GetMapping("/set/{requirementSetId}")
    public ResponseEntity<List<RequirementDTO>> getRequirementsBySet(
            @PathVariable UUID requirementSetId, @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForProject(requirementSetId, userId(jwt));
        return ResponseEntity.ok(requirementService.getRequirementsBySetId(requirementSetId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RequirementHistoryDTO>> getRequirementHistory(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForRequirement(id, userId(jwt));
        return ResponseEntity.ok(requirementService.getRequirementHistory(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RequirementDTO> updateRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForRequirement(id, userId(jwt));
        return ResponseEntity.ok(requirementService.updateRequirement(
                id, request.getRawRequirement(), request.getRefinedRequirement(), request.isUseRefinedVersion()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireOwnerOrAdminForRequirement(id, userId(jwt));
        requirementService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
