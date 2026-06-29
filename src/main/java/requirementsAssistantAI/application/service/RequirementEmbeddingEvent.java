package requirementsAssistantAI.application.service;

import java.util.UUID;

public record RequirementEmbeddingEvent(UUID requirementId, Operation operation) {
    public enum Operation { UPSERT, DELETE }
}
