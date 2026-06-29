package requirementsAssistantAI.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.AppUser;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.ChatbotEnrollment;
import requirementsAssistantAI.dto.ChatbotEnrollmentDTO;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.ChatbotEnrollmentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ChatbotEnrollmentService {

    private final ChatbotCodeService codeService;
    private final ChatbotConfigRepository chatbotRepository;
    private final ChatbotEnrollmentRepository enrollmentRepository;
    private final AppUserRepository userRepository;

    public ChatbotEnrollmentService(
            ChatbotCodeService codeService,
            ChatbotConfigRepository chatbotRepository,
            ChatbotEnrollmentRepository enrollmentRepository,
            AppUserRepository userRepository) {
        this.codeService = codeService;
        this.chatbotRepository = chatbotRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatbotEnrollmentDTO join(String rawCode, UUID userId) {
        ChatbotConfig chatbot = chatbotRepository
                .findByAccessCodeHashAndIsActiveTrue(codeService.hash(rawCode))
                .orElseThrow(() -> new ResourceNotFoundException("Código inválido ou chatbot desativado."));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        ChatbotEnrollment enrollment = enrollmentRepository
                .findByChatbot_IdAndUser_Id(chatbot.getId(), userId)
                .orElseGet(() -> enrollmentRepository.save(new ChatbotEnrollment(chatbot, user)));
        return toDTO(enrollment);
    }

    @Transactional(readOnly = true)
    public List<ChatbotEnrollmentDTO> listMine(UUID userId) {
        return enrollmentRepository.findAllByUserIdWithChatbot(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ChatbotEnrollmentDTO toDTO(ChatbotEnrollment enrollment) {
        ChatbotConfig chatbot = enrollment.getChatbot();
        return new ChatbotEnrollmentDTO(
                chatbot.getId(),
                chatbot.getName(),
                chatbot.getRequirementSet().getId(),
                chatbot.getRequirementSet().getName(),
                chatbot.getIsActive(),
                chatbot.isAvailableNow(),
                enrollment.getJoinedAt());
    }
}
