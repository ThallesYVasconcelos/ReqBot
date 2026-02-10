package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.dto.RequirementDTO;
import requirementsAssistantAI.dto.RequirementSetDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotUserService {

    private final ChatbotConfigRepository chatbotConfigRepository;
    private final RequirementService requirementService;
    private final RequirementSetService requirementSetService;

    public ChatbotUserService(
            ChatbotConfigRepository chatbotConfigRepository,
            RequirementService requirementService,
            RequirementSetService requirementSetService) {
        this.chatbotConfigRepository = chatbotConfigRepository;
        this.requirementService = requirementService;
        this.requirementSetService = requirementSetService;
    }

    @Transactional(readOnly = true)
    public RequirementSetDTO getChatbotRequirementSet() {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                .orElseThrow(() -> new RuntimeException("Chatbot não está configurado ou está inativo"));
        
        return requirementSetService.getRequirementSetById(config.getRequirementSet().getId());
    }

    @Transactional(readOnly = true)
    public List<RequirementDTO> getApprovedRequirements() {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                .orElseThrow(() -> new RuntimeException("Chatbot não está configurado ou está inativo"));
        
        List<RequirementDTO> allRequirements = requirementService.getRequirementsBySetId(
                config.getRequirementSet().getId()
        );
        
        return allRequirements.stream()
                .filter(req -> "APPROVED".equals(req.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isChatbotAvailable() {
        try {
            ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                    .orElse(null);
            return config != null && config.isAvailableNow();
        } catch (Exception e) {
            return false;
        }
    }
}
