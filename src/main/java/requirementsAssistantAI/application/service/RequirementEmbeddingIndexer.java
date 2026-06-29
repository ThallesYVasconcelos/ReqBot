package requirementsAssistantAI.application.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.infrastructure.RequirementRepository;

import java.util.Map;

@Service
public class RequirementEmbeddingIndexer {

    private static final Logger log = LoggerFactory.getLogger(RequirementEmbeddingIndexer.class);

    private final RequirementRepository requirementRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public RequirementEmbeddingIndexer(
            RequirementRepository requirementRepository,
            @Lazy EmbeddingModel embeddingModel,
            @Lazy EmbeddingStore<TextSegment> embeddingStore) {
        this.requirementRepository = requirementRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Async("embeddingTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RequirementEmbeddingEvent event) {
        try {
            remove(event);
            if (event.operation() == RequirementEmbeddingEvent.Operation.UPSERT) {
                requirementRepository.findByIdWithRequirementSet(event.requirementId())
                        .ifPresent(this::index);
            }
        } catch (RuntimeException exception) {
            log.warn("Falha ao processar embedding do requisito {}: {}",
                    event.requirementId(), exception.getMessage());
        }
    }

    private void remove(RequirementEmbeddingEvent event) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("requirement_uuid")
                .isEqualTo(event.requirementId().toString()));
    }

    private void index(Requirement requirement) {
        String body = requirement.getRefinedRequirement() != null
                ? requirement.getRefinedRequirement()
                : requirement.getRawRequirement();
        String text = requirement.getRequirementId() + ": " + body;
        Metadata metadata = Metadata.from(Map.of(
                "project_id", requirement.getRequirementSet().getId().toString(),
                "requirement_uuid", requirement.getUuid().toString()));
        TextSegment segment = TextSegment.from(text, metadata);
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
    }
}
