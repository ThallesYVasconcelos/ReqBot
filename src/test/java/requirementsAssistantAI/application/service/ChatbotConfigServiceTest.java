package requirementsAssistantAI.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceType;
import requirementsAssistantAI.dto.ChatbotConfigDTO;
import requirementsAssistantAI.dto.CreateChatbotConfigRequest;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatbotConfigServiceTest {

    private ChatbotConfigRepository chatbotRepository;
    private RequirementSetRepository projectRepository;
    private WorkspaceAuthorizationService authorizationService;
    private ChatbotAccessService accessService;
    private ChatbotConfigService service;

    @BeforeEach
    void setUp() {
        chatbotRepository = mock(ChatbotConfigRepository.class);
        projectRepository = mock(RequirementSetRepository.class);
        authorizationService = mock(WorkspaceAuthorizationService.class);
        accessService = mock(ChatbotAccessService.class);
        service = new ChatbotConfigService(
                chatbotRepository, projectRepository, authorizationService,
                accessService, new ChatbotCodeService());
    }

    @Test
    void createShouldGenerateCodeWithoutDeactivatingOtherChatbots() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.ACADEMIC);
        workspace.setId(workspaceId);
        RequirementSet project = new RequirementSet("Projeto", "Descrição", workspace);
        project.setId(projectId);
        CreateChatbotConfigRequest request = new CreateChatbotConfigRequest();
        request.setName("Chatbot da manhã");
        request.setRequirementSetId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(chatbotRepository.existsByAccessCodeHash(any())).thenReturn(false);
        when(chatbotRepository.save(any())).thenAnswer(invocation -> {
            ChatbotConfig chatbot = invocation.getArgument(0);
            chatbot.setId(UUID.randomUUID());
            return chatbot;
        });

        ChatbotConfigDTO result = service.create(request, workspaceId, userId);

        assertThat(result.getAccessCode()).matches("[A-Z2-9]{5}-[A-Z2-9]{5}");
        assertThat(result.getIsActive()).isTrue();
        ArgumentCaptor<ChatbotConfig> captor = ArgumentCaptor.forClass(ChatbotConfig.class);
        verify(chatbotRepository).save(captor.capture());
        assertThat(captor.getValue().getAccessCodeHash()).hasSize(64);
        assertThat(captor.getValue().getRequirementSet()).isSameAs(project);
        verify(authorizationService).requireOwnerOrAdmin(workspaceId, userId);
        verifyNoMoreInteractions(authorizationService);
    }

    @Test
    void createShouldStoreCodeSoItCanBeShownAgain() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Workspace workspace = new Workspace("Workspace", "DescriÃ§Ã£o", WorkspaceType.ACADEMIC);
        workspace.setId(workspaceId);
        RequirementSet project = new RequirementSet("Projeto", "DescriÃ§Ã£o", workspace);
        project.setId(projectId);
        CreateChatbotConfigRequest request = new CreateChatbotConfigRequest();
        request.setName("Chatbot");
        request.setRequirementSetId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(chatbotRepository.existsByAccessCodeHash(any())).thenReturn(false);
        when(chatbotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChatbotConfigDTO created = service.create(request, workspaceId, userId);

        ArgumentCaptor<ChatbotConfig> captor = ArgumentCaptor.forClass(ChatbotConfig.class);
        verify(chatbotRepository).save(captor.capture());
        assertThat(captor.getValue().getAccessCode()).isEqualTo(created.getAccessCode());
        assertThat(created.getAccessCode()).matches("[A-Z2-9]{5}-[A-Z2-9]{5}");
    }

    @Test
    void getAccessCodeShouldReturnExistingSingleCode() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chatbotId = UUID.randomUUID();
        ChatbotConfig chatbot = new ChatbotConfig("Chatbot", "a".repeat(64), null, null, null);
        chatbot.setId(chatbotId);
        chatbot.setAccessCode("ABCDE-23456");
        when(accessService.requireManager(workspaceId, chatbotId, userId)).thenReturn(chatbot);

        ChatbotConfigDTO result = service.getAccessCode(workspaceId, chatbotId, userId);

        assertThat(result.getAccessCode()).isEqualTo("ABCDE-23456");
        verifyNoInteractions(chatbotRepository);
    }
}
