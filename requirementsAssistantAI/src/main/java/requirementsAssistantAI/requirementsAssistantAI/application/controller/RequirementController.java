package requirementsAssistantAI.requirementsAssistantAI.application.controller;

import com.tcc.requirements_assistant_api.model.Requirement;
import com.tcc.requirements_assistant_api.model.RequirementHistory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import requirementsAssistantAI.requirementsAssistantAI.application.service.RequirementService;
import requirementsAssistantAI.requirementsAssistantAI.dto.CreateRequirementRequest;
import requirementsAssistantAI.requirementsAssistantAI.dto.RequirementDTO;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ResponseEntity<RequirementDTO> processAndSaveRequirement(@Valid @RequestBody CreateRequirementRequest request) {
        Requirement requirement = requirementService.processAndSaveRequirement(
                request.getRequirement(),
                Objects.requireNonNull(request.getRequirementSetId())
        );
        RequirementDTO dto = requirementService.getRequirementById(requirement.getUuid());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable @NonNull UUID id) {
        requirementService.approveRequirement(Objects.requireNonNull(id));
        return ResponseEntity.ok().build();
    }
}
