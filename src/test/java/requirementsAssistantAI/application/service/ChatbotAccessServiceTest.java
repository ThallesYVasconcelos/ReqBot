package requirementsAssistantAI.application.service;

import org.junit.jupiter.api.Test;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.domain.*;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.ChatbotEnrollmentRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatbotAccessServiceTest {

    @Test
    void enrolledUserShouldAccessOnlyTheJoinedChatbot() {
        ChatbotConfigRepository chatbotRepository = mock(ChatbotConfigRepository.class);
        ChatbotEnrollmentRepository enrollmentRepository = mock(ChatbotEnrollmentRepository.class);
        WorkspaceMemberRepository memberRepository = mock(WorkspaceMemberRepository.class);
        ChatbotAccessService service = new ChatbotAccessService(
                chatbotRepository, enrollmentRepository, memberRepository,
                mock(WorkspaceAuthorizationService.class));
        UUID userId = UUID.randomUUID();
        ChatbotConfig chatbot = chatbot(UUID.randomUUID());
        when(chatbotRepository.findByIdWithProjectAndWorkspace(chatbot.getId()))
                .thenReturn(Optional.of(chatbot));
        when(enrollmentRepository.existsByChatbot_IdAndUser_Id(chatbot.getId(), userId))
                .thenReturn(true);

        assertThat(service.requireChatAccess(chatbot.getId(), userId)).isSameAs(chatbot);
    }

    @Test
    void userWithoutEnrollmentShouldBeDenied() {
        ChatbotConfigRepository chatbotRepository = mock(ChatbotConfigRepository.class);
        ChatbotEnrollmentRepository enrollmentRepository = mock(ChatbotEnrollmentRepository.class);
        WorkspaceMemberRepository memberRepository = mock(WorkspaceMemberRepository.class);
        ChatbotAccessService service = new ChatbotAccessService(
                chatbotRepository, enrollmentRepository, memberRepository,
                mock(WorkspaceAuthorizationService.class));
        UUID userId = UUID.randomUUID();
        ChatbotConfig chatbot = chatbot(UUID.randomUUID());
        when(chatbotRepository.findByIdWithProjectAndWorkspace(chatbot.getId()))
                .thenReturn(Optional.of(chatbot));

        assertThatThrownBy(() -> service.requireChatAccess(chatbot.getId(), userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("código");
    }

    private ChatbotConfig chatbot(UUID id) {
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.PROFESSIONAL);
        workspace.setId(UUID.randomUUID());
        RequirementSet project = new RequirementSet("Projeto", "Descrição", workspace);
        project.setId(UUID.randomUUID());
        ChatbotConfig chatbot = new ChatbotConfig("Chatbot", "a".repeat(64), project, null, null);
        chatbot.setId(id);
        chatbot.setIsActive(true);
        return chatbot;
    }
}
