package requirementsAssistantAI.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import requirementsAssistantAI.application.service.ChatbotUserService;
import requirementsAssistantAI.dto.ChatbotScheduleDTO;
import requirementsAssistantAI.dto.RequirementDTO;
import requirementsAssistantAI.dto.RequirementSetDTO;

@RestController
@RequestMapping("/api/user")
public class ChatbotUserController {

    private final ChatbotUserService chatbotUserService;

    public ChatbotUserController(ChatbotUserService chatbotUserService) {
        this.chatbotUserService = chatbotUserService;
    }

    @GetMapping("/chatbot/requirement-set")
    public ResponseEntity<RequirementSetDTO> getChatbotRequirementSet() {
        RequirementSetDTO requirementSet = chatbotUserService.getChatbotRequirementSet();
        return ResponseEntity.ok(requirementSet);
    }

    @GetMapping("/chatbot/requirements/approved")
    public ResponseEntity<List<RequirementDTO>> getApprovedRequirements() {
        List<RequirementDTO> requirements = chatbotUserService.getApprovedRequirements();
        return ResponseEntity.ok(requirements);
    }

    @GetMapping("/chatbot/schedule")
    public ResponseEntity<ChatbotScheduleDTO> getSchedule() {
        ChatbotScheduleDTO schedule = chatbotUserService.getChatbotSchedule();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/chatbot/availability")
    public ResponseEntity<Boolean> getAvailability() {
        boolean isAvailable = chatbotUserService.isChatbotAvailable();
        return ResponseEntity.ok(isAvailable);
    }
}
