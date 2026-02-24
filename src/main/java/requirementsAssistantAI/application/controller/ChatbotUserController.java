package requirementsAssistantAI.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import requirementsAssistantAI.application.service.ChatbotUserService;
import requirementsAssistantAI.dto.ChatbotScheduleDTO;
import requirementsAssistantAI.dto.RequirementDTO;
import requirementsAssistantAI.dto.RequirementSetDTO;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;

@RestController
@RequestMapping("/api/user")
public class ChatbotUserController {

    private final ChatbotUserService chatbotUserService;
    private final ChatbotConfigRepository chatbotConfigRepository;

    public ChatbotUserController(ChatbotUserService chatbotUserService,
                                ChatbotConfigRepository chatbotConfigRepository) {
        this.chatbotUserService = chatbotUserService;
        this.chatbotConfigRepository = chatbotConfigRepository;
    }

    @GetMapping("/chatbot/requirement-set")
    public ResponseEntity<RequirementSetDTO> getChatbotRequirementSet() {
        RequirementSetDTO requirementSet = chatbotUserService.getChatbotRequirementSet();
        return ResponseEntity.ok(requirementSet);
    }

    @GetMapping("/chatbot/requirements/approved")
    public ResponseEntity<?> getApprovedRequirements(Authentication auth) {
        if (!canSeeRequirements(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado: requisitos não estão disponíveis para visualização.");
        }
        List<RequirementDTO> requirements = chatbotUserService.getApprovedRequirements();
        return ResponseEntity.ok(requirements);
    }

    private boolean canSeeRequirements(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return true;
        return chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                .map(c -> Boolean.TRUE.equals(c.getShowRequirementsToUsers()))
                .orElse(false);
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
