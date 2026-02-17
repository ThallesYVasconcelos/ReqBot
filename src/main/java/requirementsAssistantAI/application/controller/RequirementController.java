package requirementsAssistantAI.application.controller;

import requirementsAssistantAI.domain.RequirementHistory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import requirementsAssistantAI.application.service.RequirementService;
import requirementsAssistantAI.dto.CreateRequirementRequest;
import requirementsAssistantAI.dto.RefineRequirementRequest;
import requirementsAssistantAI.dto.RequirementDTO;
import requirementsAssistantAI.dto.RequirementReportDTO;
import requirementsAssistantAI.dto.SaveRequirementRequest;
import requirementsAssistantAI.dto.UpdateRequirementRequest;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    /**
     * Refina requisição com IA sem salvar. Usuário edita e escolhe versão antes de salvar.
     */
    @PostMapping
    public ResponseEntity<RequirementDTO> refineRequirement(@Valid @RequestBody CreateRequirementRequest request) {
        RequirementDTO dto = requirementService.refineRequirement(
                request.getRequirement(),
                Objects.requireNonNull(request.getRequirementSetId())
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/refine")
    public ResponseEntity<RequirementDTO> refine(@Valid @RequestBody RefineRequirementRequest request) {
        RequirementDTO dto = requirementService.refineRequirement(
                request.getRequirement(),
                Objects.requireNonNull(request.getRequirementSetId())
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/save")
    public ResponseEntity<RequirementDTO> saveRequirement(@Valid @RequestBody SaveRequirementRequest request) {
        RequirementDTO dto = requirementService.saveRequirement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<RequirementDTO>> getAllRequirements(
            @RequestParam(required = false) UUID requirementSetId) {
        List<RequirementDTO> requirements = requirementService.getAllRequirements(requirementSetId);
        return ResponseEntity.ok(requirements);
    }

    @GetMapping("/report")
    public ResponseEntity<RequirementReportDTO> getGeneralReport(
            @RequestParam @NonNull UUID requirementSetId) {
        RequirementReportDTO report = requirementService.getGeneralReport(Objects.requireNonNull(requirementSetId));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequirementDTO> getRequirement(@PathVariable @NonNull UUID id) {
        RequirementDTO requirementDTO = requirementService.getRequirementById(Objects.requireNonNull(id));
        return ResponseEntity.ok(requirementDTO);
    }

    @GetMapping("/set/{requirementSetId}")
    public ResponseEntity<List<RequirementDTO>> getRequirementsBySet(@PathVariable @NonNull UUID requirementSetId) {
        List<RequirementDTO> requirements = requirementService.getRequirementsBySetId(Objects.requireNonNull(requirementSetId));
        return ResponseEntity.ok(requirements);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RequirementHistory>> getRequirementHistory(@PathVariable @NonNull UUID id) {
        List<RequirementHistory> history = requirementService.getRequirementHistory(Objects.requireNonNull(id));
        return ResponseEntity.ok(history);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RequirementDTO> updateRequirement(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody UpdateRequirementRequest request) {
        RequirementDTO dto = requirementService.updateRequirement(
                Objects.requireNonNull(id),
                request.getRawRequirement(),
                request.getRefinedRequirement(),
                request.isUseRefinedVersion()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequirement(@PathVariable @NonNull UUID id) {
        requirementService.deleteRequirement(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }
}
