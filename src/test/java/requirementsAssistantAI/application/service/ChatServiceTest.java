package requirementsAssistantAI.application.service;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ChatMessageRepository chatMessageRepository;
    private ChatAiService chatAiService;
    private ChatbotAccessService accessService;
    private RagRetrievalService ragRetrievalService;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatAiService = mock(ChatAiService.class);
        accessService = mock(ChatbotAccessService.class);
        ragRetrievalService = mock(RagRetrievalService.class);

        chatService = new ChatService(
                chatMessageRepository, chatAiService, accessService, ragRetrievalService);
    }

    @Test
    void questionShouldAuthorizeAndPersistChatbotContext() {
        UUID userId = UUID.randomUUID();
        UUID chatbotId = UUID.randomUUID();
        ChatbotConfig chatbot = chatbot(chatbotId);
        UUID projectId = chatbot.getRequirementSet().getId();
        when(accessService.requireChatAccess(chatbotId, userId)).thenReturn(chatbot);
        when(ragRetrievalService.retrieve(any(), eq(projectId), anyInt(), anyDouble(), anyInt()))
                .thenReturn("Nenhum requisito salvo encontrado para este projeto.");
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
        when(ragRetrievalService.retrieve(any(), any(), anyInt(), anyDouble(), anyInt()))
                .thenReturn("Contexto");
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
