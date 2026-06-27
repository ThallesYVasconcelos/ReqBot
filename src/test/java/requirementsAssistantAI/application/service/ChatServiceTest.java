package requirementsAssistantAI.application.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import requirementsAssistantAI.application.ports.ChatAiService;
import requirementsAssistantAI.domain.ChatMessage;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceType;
import requirementsAssistantAI.dto.ChatResponseDTO;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.RequirementRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ChatbotConfigRepository chatbotConfigRepository;
    private RequirementRepository requirementRepository;
    private ChatMessageRepository chatMessageRepository;
    private ChatAiService chatAiService;
    private WorkspaceAuthorizationService authorizationService;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatbotConfigRepository = mock(ChatbotConfigRepository.class);
        requirementRepository = mock(RequirementRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatAiService = mock(ChatAiService.class);
        authorizationService = mock(WorkspaceAuthorizationService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);

        chatService = new ChatService(
                chatbotConfigRepository, requirementRepository, chatMessageRepository,
                chatAiService, embeddingModel, embeddingStore, authorizationService);
    }

    @Test
    void workspaceQuestionShouldAuthorizeAndPersistTenantContext() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Workspace workspace = new Workspace(
                "Workspace de Banco de Dados", "Regras de negócio", WorkspaceType.ACADEMIC);
        workspace.setId(workspaceId);
        RequirementSet project = new RequirementSet("Medicamentos", "Domínio de farmácia", workspace);
        project.setId(projectId);
        ChatbotConfig config = new ChatbotConfig(project, null, null);
        config.setIsActive(true);

        when(chatbotConfigRepository.findByIsActiveTrueAndWorkspace_Id(workspaceId))
                .thenReturn(Optional.of(config));
        when(requirementRepository.countByRequirementSetId(projectId)).thenReturn(0L);
        when(chatAiService.answerQuestion(
                eq("Qual regra vale para a cor da caixa?"),
                eq("Nenhum requisito salvo encontrado para este projeto.")))
                .thenReturn("A cor é uma característica do produto.");

        ChatResponseDTO response = chatService.answerQuestion(
                "Qual regra vale para a cor da caixa?", userId,
                "aluno@example.com", workspaceId);

        assertThat(response.getSuccess()).isTrue();
        verify(authorizationService).requireOwnerOrAdmin(workspaceId, userId);
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getWorkspace()).isSameAs(workspace);
        assertThat(captor.getValue().getRequirementSet()).isSameAs(project);
        assertThat(captor.getValue().getUserEmail()).isEqualTo("aluno@example.com");
    }

    @Test
    void workspaceQuestionShouldPersistFriendlyErrorWhenAiFails() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.PROFESSIONAL);
        workspace.setId(workspaceId);
        RequirementSet project = new RequirementSet("Projeto", "Descrição", workspace);
        project.setId(projectId);
        ChatbotConfig config = new ChatbotConfig(project, null, null);
        config.setIsActive(true);

        when(chatbotConfigRepository.findByIsActiveTrueAndWorkspace_Id(workspaceId))
                .thenReturn(Optional.of(config));
        when(requirementRepository.countByRequirementSetId(projectId)).thenReturn(0L);
        when(chatAiService.answerQuestion(any(), any()))
                .thenThrow(new RuntimeException("IA indisponível"));

        ChatResponseDTO response = chatService.answerQuestion(
                "Pergunta", userId, "aluno@example.com", workspaceId);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getAnswer()).contains("ocorreu um erro");
        verify(chatMessageRepository).save(argThat(message ->
                message.getAnswer().contains("ocorreu um erro")));
    }
}
