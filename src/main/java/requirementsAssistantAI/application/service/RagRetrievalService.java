package requirementsAssistantAI.application.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.infrastructure.RequirementRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalService.class);

    @Value("${ai.rag.literal-sufficient-results:2}")
    private int literalSufficientResults;

    private final RequirementRepository requirementRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public RagRetrievalService(
            RequirementRepository requirementRepository,
            @Lazy EmbeddingModel embeddingModel,
            @Lazy EmbeddingStore<TextSegment> embeddingStore) {
        this.requirementRepository = requirementRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Recuperação híbrida econômica: busca textual primeiro e só chama a API de
     * embeddings quando os resultados literais não forem suficientes.
     */
    @Transactional(readOnly = true)
    public String retrieve(
            String query, UUID projectId, int maxResults, double minSemanticScore, int maxLength) {
        int safeResults = Math.max(1, Math.min(maxResults, 10));
        int safeLength = Math.max(200, Math.min(maxLength, 10_000));
        if (requirementRepository.countByRequirementSetId(projectId) == 0) {
            return "";
        }

        Set<String> contexts = new LinkedHashSet<>();
        List<Requirement> literalMatches = literalMatches(query, projectId, safeResults);
        literalMatches.stream().map(this::requirementText).forEach(contexts::add);

        if (literalMatches.size() < Math.max(1, literalSufficientResults)) {
            semanticMatches(query, projectId, safeResults, minSemanticScore)
                    .forEach(contexts::add);
        }
        if (contexts.size() < Math.max(1, literalSufficientResults)) {
            lexicalFallbackMatches(query, projectId, safeResults).stream()
                    .map(this::requirementText)
                    .forEach(contexts::add);
        }

        return joinWithinLimit(contexts, safeResults, safeLength);
    }

    private List<Requirement> literalMatches(String query, UUID projectId, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return requirementRepository.searchLiteral(projectId, query.trim(), limit);
        } catch (RuntimeException exception) {
            log.warn("Busca textual indisponível para o projeto {}: {}", projectId, exception.getMessage());
            return List.of();
        }
    }

    private List<Requirement> lexicalFallbackMatches(String query, UUID projectId, int limit) {
        Set<String> queryTokens = normalizedSearchTokens(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        List<ScoredRequirement> scored = new ArrayList<>();
        for (Requirement requirement : requirementRepository.findByRequirementSet_Id(projectId)) {
            Set<String> requirementTokens = normalizedSearchTokens(requirementText(requirement));
            long matched = queryTokens.stream()
                    .filter(queryToken -> matchesAnyRequirementToken(queryToken, requirementTokens))
                    .count();
            if (matched > 0) {
                scored.add(new ScoredRequirement(requirement, matched / (double) queryTokens.size()));
            }
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredRequirement::score).reversed())
                .limit(limit)
                .map(ScoredRequirement::requirement)
                .toList();
    }

    private Set<String> normalizedSearchTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        for (String rawToken : normalized.split("\\s+")) {
            String token = rootToken(rawToken);
            if (token.length() >= 3 && !isStopWord(token)) {
                tokens.add(token);
                if ("adm".equals(token) || "admin".equals(token)) {
                    tokens.add("administr");
                }
            }
        }
        return tokens;
    }

    private String rootToken(String token) {
        String value = token.replaceAll("[^a-z0-9]", "");
        if (value.length() <= 4) {
            return value;
        }
        value = value.replaceAll("(adores|adora|ador|acoes|acao|mente|ando|endo|indo|ados|adas|ado|ada|ar|er|ir|es|s)$", "");
        return value.length() >= 4 ? value : token;
    }

    private boolean isStopWord(String token) {
        return Set.of(
                "como", "deve", "devo", "qual", "quais", "isso", "esse", "essa", "este", "esta",
                "para", "por", "com", "que", "uma", "uns", "das", "dos", "nas", "nos", "ser")
                .contains(token);
    }

    private boolean matchesAnyRequirementToken(String queryToken, Set<String> requirementTokens) {
        return requirementTokens.stream()
                .anyMatch(requirementToken -> requirementToken.equals(queryToken)
                        || requirementToken.startsWith(queryToken)
                        || queryToken.startsWith(requirementToken));
    }

    private List<String> semanticMatches(
            String query, UUID projectId, int limit, double minSemanticScore) {
        try {
            Embedding embedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(limit)
                    .minScore(Math.max(0.0, Math.min(minSemanticScore, 1.0)))
                    .filter(MetadataFilterBuilder.metadataKey("project_id")
                            .isEqualTo(projectId.toString()))
                    .build();
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            return result.matches().stream()
                    .map(match -> match.embedded().text())
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Busca semântica indisponível para o projeto {}: {}", projectId, exception.getMessage());
            return List.of();
        }
    }

    private String requirementText(Requirement requirement) {
        String body = requirement.getRefinedRequirement();
        if (body == null || body.isBlank()) {
            body = requirement.getRawRequirement();
        }
        return (requirement.getRequirementId() != null ? requirement.getRequirementId() + ": " : "") +
                (body != null ? body : "");
    }

    private String joinWithinLimit(Set<String> contexts, int maxResults, int maxLength) {
        StringBuilder value = new StringBuilder();
        int used = 0;
        for (String context : contexts) {
            if (context == null || context.isBlank() || used >= maxResults) {
                continue;
            }
            String separator = value.isEmpty() ? "" : "\n---\n";
            int remaining = maxLength - value.length() - separator.length();
            if (remaining <= 0) {
                break;
            }
            value.append(separator);
            value.append(context, 0, Math.min(context.length(), remaining));
            used++;
            if (context.length() > remaining) {
                break;
            }
        }
        return value.toString();
    }

    private record ScoredRequirement(Requirement requirement, double score) {}
}
