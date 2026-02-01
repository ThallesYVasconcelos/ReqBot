package requirementsAssistantAI.requirementsAssistantAI.application.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import requirementsAssistantAI.requirementsAssistantAI.application.ports.ChatAiService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço do chatbot RAG com filtro por projeto.
 * Busca vetorial no EmbeddingStore restrita aos requisitos aprovados do projeto.
 */
@Service
public class ChatService {

    private final ChatAiService chatAiService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ChatService(ChatAiService chatAiService,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore) {
        this.chatAiService = chatAiService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Responde uma pergunta do usuário com base nos requisitos aprovados do projeto.
     * O filtro por project_id garante que apenas requisitos do projeto atual sejam considerados.
     */
    public String answerQuestion(@NonNull String question, @NonNull UUID projectId) {
        Objects.requireNonNull(question, "A pergunta não pode ser nula");
        Objects.requireNonNull(projectId, "O ID do projeto não pode ser nulo");

        String projectIdStr = projectId.toString();
        String context = findRelevantContext(question, projectIdStr);

        return chatAiService.answerQuestion(question, context);
    }

    private String findRelevantContext(String query, String projectId) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .filter(MetadataFilterBuilder.metadataKey("project_id").isEqualTo(projectId))
                .maxResults(5)
                .minScore(0.6)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<String> texts = result.matches().stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.toList());

        if (texts.isEmpty()) {
            return "Nenhum requisito aprovado encontrado para este projeto.";
        }

        return String.join("\n---\n", texts);
    }
}
