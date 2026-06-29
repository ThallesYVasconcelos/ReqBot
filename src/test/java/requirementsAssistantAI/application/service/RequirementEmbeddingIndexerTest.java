package requirementsAssistantAI.application.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.junit.jupiter.api.Test;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.infrastructure.RequirementRepository;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequirementEmbeddingIndexerTest {

    @Test
    void upsertShouldReplaceStoredEmbeddingUsingPersistedRequirement() {
        RequirementRepository repository = mock(RequirementRepository.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> store = mock(EmbeddingStore.class);
        RequirementEmbeddingIndexer indexer = new RequirementEmbeddingIndexer(repository, model, store);
        UUID requirementId = UUID.randomUUID();
        RequirementSet project = new RequirementSet("Projeto", "Descrição");
        project.setId(UUID.randomUUID());
        Requirement requirement = new Requirement("REQ-001", project);
        requirement.setUuid(requirementId);
        requirement.setRefinedRequirement("O sistema deve autenticar o usuário.");
        when(repository.findByIdWithRequirementSet(requirementId))
                .thenReturn(Optional.of(requirement));
        when(model.embed(any(TextSegment.class)))
                .thenReturn(Response.from(Embedding.from(new float[]{1, 0})));

        indexer.handle(new RequirementEmbeddingEvent(
                requirementId, RequirementEmbeddingEvent.Operation.UPSERT));

        verify(store).removeAll(any(Filter.class));
        verify(store).add(any(Embedding.class), any(TextSegment.class));
    }
}
