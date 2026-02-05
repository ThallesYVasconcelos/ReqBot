package requirementsAssistantAI.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import requirementsAssistantAI.application.service.RequirementSetService;
import requirementsAssistantAI.dto.CreateRequirementSetRequest;
import requirementsAssistantAI.dto.RequirementSetDTO;
import requirementsAssistantAI.dto.RequirementSummaryDTO;

@RestController
@RequestMapping("/api/requirement-sets")
public class RequirementSetController {

    private final RequirementSetService requirementSetService;

    public RequirementSetController(RequirementSetService requirementSetService) {
        this.requirementSetService = requirementSetService;
    }

    @PostMapping
    public ResponseEntity<RequirementSetDTO> createRequirementSet(@Valid @RequestBody CreateRequirementSetRequest request) {
        RequirementSetDTO requirementSetDTO = requirementSetService.createRequirementSet(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(requirementSetDTO);
    }

    @GetMapping
    public ResponseEntity<List<RequirementSetDTO>> getAllRequirementSets() {
        List<RequirementSetDTO> requirementSets = requirementSetService.getAllRequirementSets();
        return ResponseEntity.ok(requirementSets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequirementSetDTO> getRequirementSet(@PathVariable @NonNull UUID id) {
        RequirementSetDTO requirementSetDTO = requirementSetService.getRequirementSetById(Objects.requireNonNull(id));
        return ResponseEntity.ok(requirementSetDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequirementSet(@PathVariable @NonNull UUID id) {
        requirementSetService.deleteRequirementSet(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/requirements")
    public ResponseEntity<List<RequirementSummaryDTO>> getRequirementsBySetId(@PathVariable @NonNull UUID id) {
        List<RequirementSummaryDTO> requirements = requirementSetService.getRequirementsBySetId(Objects.requireNonNull(id));
        return ResponseEntity.ok(requirements);
    }
}
