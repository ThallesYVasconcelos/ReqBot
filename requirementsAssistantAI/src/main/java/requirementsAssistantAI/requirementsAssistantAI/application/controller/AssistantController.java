package requirementsAssistantAI.requirementsAssistantAI.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import requirementsAssistantAI.requirementsAssistantAI.application.service.RequirementService;
import requirementsAssistantAI.requirementsAssistantAI.dto.CreateRequirementRequest;
import requirementsAssistantAI.requirementsAssistantAI.dto.RequirementDTO;

@RestController
@RequestMapping("/api/requirements-assistant")
public class AssistantController {

    private final RequirementService requirementService;

    public AssistantController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ResponseEntity<RequirementDTO> createRequirement(@Valid @RequestBody CreateRequirementRequest request) {
        RequirementDTO dto = requirementService.processAndSaveRequirementAsDTO(
                request.getRequirement(),
                Objects.requireNonNull(request.getRequirementSetId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
