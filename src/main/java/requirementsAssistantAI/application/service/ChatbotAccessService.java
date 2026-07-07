package requirementsAssistantAI.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.ChatbotEnrollmentRepository;
import requirementsAssistantAI.infrastructure.ProjectEnrollmentRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;

import java.util.UUID;

@Service
public class ChatbotAccessService {

    private final ChatbotConfigRepository chatbotRepository;
    private final ChatbotEnrollmentRepository enrollmentRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectEnrollmentRepository projectEnrollmentRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public ChatbotAccessService(
            ChatbotConfigRepository chatbotRepository,
            ChatbotEnrollmentRepository enrollmentRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ProjectEnrollmentRepository projectEnrollmentRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.chatbotRepository = chatbotRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.projectEnrollmentRepository = projectEnrollmentRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Transactional(readOnly = true)
    public ChatbotConfig requireManager(UUID workspaceId, UUID chatbotId, UUID userId) {
        ChatbotConfig chatbot = find(chatbotId);
        if (chatbot.getWorkspace() == null || !chatbot.getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Chatbot", chatbotId);
        }
        workspaceAuthorizationService.requireOwnerOrAdmin(workspaceId, userId);
        return chatbot;
    }

    @Transactional(readOnly = true)
    public ChatbotConfig requireChatAccess(UUID chatbotId, UUID userId) {
        ChatbotConfig chatbot = find(chatbotId);
        if (!Boolean.TRUE.equals(chatbot.getIsActive())) {
            throw new ForbiddenException("Este chatbot está desativado.");
        }
        UUID workspaceId = chatbot.getWorkspace().getId();
        boolean manager = workspaceMemberRepository
                .existsByWorkspace_IdAndUser_Id(workspaceId, userId);
        boolean enrolled = enrollmentRepository.existsByChatbot_IdAndUser_Id(chatbotId, userId);
        boolean projectEnrolled = chatbot.getRequirementSet() != null
                && projectEnrollmentRepository.existsByProject_IdAndUser_Id(
                        chatbot.getRequirementSet().getId(), userId);
        if (!manager && !enrolled && !projectEnrolled) {
            throw new ForbiddenException("Informe o código para entrar neste chatbot.");
        }
        return chatbot;
    }

    private ChatbotConfig find(UUID chatbotId) {
        return chatbotRepository.findByIdWithProjectAndWorkspace(chatbotId)
                .orElseThrow(() -> new ResourceNotFoundException("Chatbot", chatbotId));
    }
}
