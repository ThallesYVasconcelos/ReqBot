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
import requirementsAssistantAI.infrastructure.RequirementRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private RequirementRepository requirementRepository;
    private ChatMessageRepository chatMessageRepository;
    private ChatAiService chatAiService;
    private ChatbotAccessService accessService;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        requirementRepository = mock(RequirementRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatAiService = mock(ChatAiService.class);
        accessService = mock(ChatbotAccessService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);

        chatService = new ChatService(
                requirementRepository, chatMessageRepository, chatAiService,
                embeddingModel, embeddingStore, accessService);
    }

    @Test
    void questionShouldAuthorizeAndPersistChatbotContext() {
        UUID userId = UUID.randomUUID();
        UUID chatbotId = UUID.randomUUID();
        ChatbotConfig chatbot = chatbot(chatbotId);
        UUID projectId = chatbot.getRequirementSet().getId();
        when(accessService.requireChatAccess(chatbotId, userId)).thenReturn(chatbot);
        when(requirementRepository.countByRequirementSetId(projectId)).thenReturn(0L);
        when(chatAiService.answerQuestion(
                eq("Qual regra vale para a cor da caixa?"),
                eq("Nenhum requisito salvo encontrado para este projeto.")))
                .thenReturn("A cor é uma característica do produto.");

        ChatResponseDTO response = chatService.answerQuestion(
                "Qual regra vale para a cor da caixa?", userId,
                "aluno@example.com", chatbotId);

        assertThat(response.getSuccess()).isTrue();
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getChatbot()).isSameAs(chatbot);
        assertThat(captor.getValue().getRequirementSet()).isSameAs(chatbot.getRequirementSet());
        assertThat(captor.getValue().getUserEmail()).isEqualTo("aluno@example.com");
    }

    @Test
    void questionShouldPersistFriendlyErrorWhenAiFails() {
        UUID userId = UUID.randomUUID();
        UUID chatbotId = UUID.randomUUID();
        ChatbotConfig chatbot = chatbot(chatbotId);
        when(accessService.requireChatAccess(chatbotId, userId)).thenReturn(chatbot);
        when(requirementRepository.countByRequirementSetId(chatbot.getRequirementSet().getId()))
                .thenReturn(0L);
        when(chatAiService.answerQuestion(any(), any()))
                .thenThrow(new RuntimeException("IA indisponível"));

        ChatResponseDTO response = chatService.answerQuestion(
                "Pergunta", userId, "aluno@example.com", chatbotId);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getAnswer()).contains("ocorreu um erro");
        verify(chatMessageRepository).save(argThat(message ->
                message.getChatbot() == chatbot && message.getAnswer().contains("ocorreu um erro")));
    }

    private ChatbotConfig chatbot(UUID chatbotId) {
        Workspace workspace = new Workspace("Workspace", "Descrição", WorkspaceType.PROFESSIONAL);
        workspace.setId(UUID.randomUUID());
        RequirementSet project = new RequirementSet("Projeto", "Descrição", workspace);
        project.setId(UUID.randomUUID());
        ChatbotConfig chatbot = new ChatbotConfig(
                "Chatbot A", "a".repeat(64), project, null, null);
        chatbot.setId(chatbotId);
        chatbot.setIsActive(true);
        return chatbot;
    }
}
