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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private ChatbotConfigRepository chatbotConfigRepository;
    private RequirementRepository requirementRepository;
    private ChatMessageRepository chatMessageRepository;
    private ChatAiService chatAiService;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatbotConfigRepository = mock(ChatbotConfigRepository.class);
        requirementRepository = mock(RequirementRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatAiService = mock(ChatAiService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);

        chatService = new ChatService(
                chatbotConfigRepository,
                requirementRepository,
                chatMessageRepository,
                chatAiService,
                embeddingModel,
                embeddingStore
        );
    }

    @Test
    void answerQuestionShouldPersistQuestionAndAnswerWithWorkspaceContext() {
        UUID requirementSetId = UUID.randomUUID();
        Workspace workspace = new Workspace(
                "Turma Banco de Dados",
                "Atividade sobre regras de negócio",
                WorkspaceType.ACADEMIC,
                "professor@example.com"
        );
        workspace.setId(UUID.randomUUID());

        RequirementSet requirementSet = new RequirementSet("Sistema de Medicamentos", "Domínio de farmácia", workspace);
        requirementSet.setId(requirementSetId);

        ChatbotConfig config = new ChatbotConfig(requirementSet, null, null);
        config.setIsActive(true);

        when(chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()).thenReturn(Optional.of(config));
        when(requirementRepository.countByRequirementSetId(requirementSetId)).thenReturn(0L);
        when(chatAiService.answerQuestion(
                eq("Qual regra vale para a cor da caixa?"),
                eq("Nenhum requisito salvo encontrado para este projeto.")
        )).thenReturn("A cor deve ser entendida como uma característica do produto no negócio.");

        ChatResponseDTO response = chatService.answerQuestion(
                "Qual regra vale para a cor da caixa?",
                "student@example.com"
        );

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getAnswer()).contains("característica do produto");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());

        ChatMessage saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo("student@example.com");
        assertThat(saved.getQuestion()).isEqualTo("Qual regra vale para a cor da caixa?");
        assertThat(saved.getAnswer()).contains("característica do produto");
        assertThat(saved.getRequirementSet()).isSameAs(requirementSet);
        assertThat(saved.getWorkspace()).isSameAs(workspace);
        assertThat(saved.getAnsweredFromCache()).isFalse();
        assertThat(saved.getChatbotAvailable()).isTrue();
    }

    @Test
    void answerQuestionShouldPersistFriendlyErrorWhenAiFails() {
        UUID requirementSetId = UUID.randomUUID();
        RequirementSet requirementSet = new RequirementSet("Projeto", "Descrição");
        requirementSet.setId(requirementSetId);
        ChatbotConfig config = new ChatbotConfig(requirementSet, null, null);

        when(chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()).thenReturn(Optional.of(config));
        when(requirementRepository.countByRequirementSetId(requirementSetId)).thenReturn(0L);
        when(chatAiService.answerQuestion(any(), any())).thenThrow(new RuntimeException("IA indisponível"));

        ChatResponseDTO response = chatService.answerQuestion("Pergunta", "student@example.com");

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getAnswer()).contains("ocorreu um erro");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getAnswer()).contains("ocorreu um erro");
        assertThat(captor.getValue().getUserEmail()).isEqualTo("student@example.com");
    }
}
