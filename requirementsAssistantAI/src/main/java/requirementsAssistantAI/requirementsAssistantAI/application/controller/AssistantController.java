package requirementsAssistantAI.requirementsAssistantAI.application.controller;

import com.tcc.requirements_assistant_api.model.Requirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import requirementsAssistantAI.requirementsAssistantAI.application.service.RequirementService;
import requirementsAssistantAI.requirementsAssistantAI.dto.CreateRequirementRequest;

@RestController
@RequestMapping("/api/requirements-assistant")
public class AssistantController {

    private final RequirementService requirementService;

    public AssistantController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ResponseEntity<Requirement> createRequirement(@Valid @RequestBody CreateRequirementRequest request) {
        Requirement requirement = requirementService.processAndSaveRequirement(
                request.getRequirement(),
                request.getRequirementSetId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(requirement);
    }
}
