package requirementsAssistantAI.application.service;

import org.junit.jupiter.api.Test;
import requirementsAssistantAI.domain.*;
import requirementsAssistantAI.dto.ChatbotEnrollmentDTO;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.ChatbotEnrollmentRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatbotEnrollmentServiceTest {

    @Test
    void joinShouldAssociateUserWithChatbotWithoutWorkspaceMembership() {
        ChatbotCodeService codeService = new ChatbotCodeService();
        ChatbotConfigRepository chatbotRepository = mock(ChatbotConfigRepository.class);
        ChatbotEnrollmentRepository enrollmentRepository = mock(ChatbotEnrollmentRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        ChatbotEnrollmentService service = new ChatbotEnrollmentService(
                codeService, chatbotRepository, enrollmentRepository, userRepository);
        UUID userId = UUID.randomUUID();
        String code = "ABCDE-23456";
        ChatbotConfig chatbot = chatbot(UUID.randomUUID(), codeService.hash(code));
        AppUser user = new AppUser("user@example.com", "User", null, AuthRole.USER);
        user.setId(userId);

        when(chatbotRepository.findByAccessCodeHashAndIsActiveTrue(codeService.hash(code)))
                .thenReturn(Optional.of(chatbot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findByChatbot_IdAndUser_Id(chatbot.getId(), userId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> {
            ChatbotEnrollment enrollment = invocation.getArgument(0);
            enrollment.setId(UUID.randomUUID());
            enrollment.setJoinedAt(LocalDateTime.now());
            return enrollment;
        });

        ChatbotEnrollmentDTO result = service.join(" abcde 23456 ", userId);

        assertThat(result.chatbotId()).isEqualTo(chatbot.getId());
        verify(enrollmentRepository).save(argThat(enrollment ->
                enrollment.getUser() == user && enrollment.getChatbot() == chatbot));
    }

    private ChatbotConfig chatbot(UUID id, String hash) {
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.PROFESSIONAL);
        workspace.setId(UUID.randomUUID());
        RequirementSet project = new RequirementSet("Projeto", "Descrição", workspace);
        project.setId(UUID.randomUUID());
        ChatbotConfig chatbot = new ChatbotConfig("Chatbot", hash, project, null, null);
        chatbot.setId(id);
        chatbot.setIsActive(true);
        return chatbot;
    }
}
