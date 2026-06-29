package requirementsAssistantAI.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class EmbeddingSimilarityRepository {

    private static final String FIND_SIMILAR_PAIRS = """
            SELECT
                (left_embedding.metadata ->> 'requirement_uuid')::uuid AS left_id,
                (right_embedding.metadata ->> 'requirement_uuid')::uuid AS right_id,
                1 - (left_embedding.embedding <=> right_embedding.embedding) AS score
            FROM embeddings left_embedding
            JOIN embeddings right_embedding
              ON left_embedding.embedding_id < right_embedding.embedding_id
            WHERE left_embedding.metadata ->> 'project_id' = :projectId
              AND right_embedding.metadata ->> 'project_id' = :projectId
              AND left_embedding.metadata ->> 'requirement_uuid' IS NOT NULL
              AND right_embedding.metadata ->> 'requirement_uuid' IS NOT NULL
              AND 1 - (left_embedding.embedding <=> right_embedding.embedding) >= :threshold
            ORDER BY score DESC
            LIMIT :maxPairs
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EmbeddingSimilarityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SimilarityPair> findSimilarPairs(UUID projectId, double threshold, int maxPairs) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", projectId.toString())
                .addValue("threshold", threshold)
                .addValue("maxPairs", maxPairs);
        return jdbcTemplate.query(FIND_SIMILAR_PAIRS, parameters, (resultSet, rowNumber) ->
                new SimilarityPair(
                        resultSet.getObject("left_id", UUID.class),
                        resultSet.getObject("right_id", UUID.class),
                        resultSet.getDouble("score")));
    }

    public record SimilarityPair(UUID leftId, UUID rightId, double score) {}
}
