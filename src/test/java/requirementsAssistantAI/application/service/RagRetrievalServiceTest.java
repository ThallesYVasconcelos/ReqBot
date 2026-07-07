package requirementsAssistantAI.application.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.infrastructure.RequirementRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RagRetrievalServiceTest {

    @Test
    void literalResultsShouldAvoidEmbeddingApiCall() {
        RequirementRepository repository = mock(RequirementRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        RagRetrievalService service = new RagRetrievalService(
                repository, embeddingModel, embeddingStore);
        ReflectionTestUtils.setField(service, "literalSufficientResults", 2);
        UUID projectId = UUID.randomUUID();
        Requirement first = requirement("REQ-001", "O medicamento possui uma cor de embalagem.");
        Requirement second = requirement("REQ-002", "A embalagem identifica o fabricante.");
        when(repository.countByRequirementSetId(projectId)).thenReturn(2L);
        when(repository.searchLiteral(projectId, "cor embalagem", 3))
                .thenReturn(List.of(first, second));

        String context = service.retrieve("cor embalagem", projectId, 3, 0.6, 1000);

        assertThat(context).contains("REQ-001", "REQ-002");
        verifyNoInteractions(embeddingModel, embeddingStore);
    }

    @Test
    void lexicalFallbackShouldMatchAdminAbbreviationWhenSearchesMiss() {
        RequirementRepository repository = mock(RequirementRepository.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        RagRetrievalService service = new RagRetrievalService(
                repository, embeddingModel, embeddingStore);
        ReflectionTestUtils.setField(service, "literalSufficientResults", 2);
        UUID projectId = UUID.randomUUID();
        Requirement unrelated = requirement("REQ-001", "Preciso de uma tela para cadastrar usuario.");
        Requirement admin = requirement("REQ-002",
                "Como administrador do sistema, quero cadastrar novos administradores com e-mail e nome.");
        when(repository.countByRequirementSetId(projectId)).thenReturn(2L);
        when(repository.searchLiteral(projectId, "COMO eu devo cadastrar adm", 3))
                .thenReturn(List.of());
        when(repository.findByRequirementSet_Id(projectId)).thenReturn(List.of(unrelated, admin));
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("offline"));

        String context = service.retrieve("COMO eu devo cadastrar adm", projectId, 3, 0.6, 1000);

        assertThat(context).contains("REQ-002", "administradores");
    }

    private Requirement requirement(String id, String text) {
        Requirement requirement = new Requirement();
        requirement.setRequirementId(id);
        requirement.setRefinedRequirement(text);
        return requirement;
    }
}
