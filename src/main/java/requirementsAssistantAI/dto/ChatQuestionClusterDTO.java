package requirementsAssistantAI.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatQuestionClusterDTO(
        int rank,
        String representativeQuestion,
        long totalOccurrences,
        List<String> similarQuestionsSample,
        LocalDateTime firstAskedAt,
        LocalDateTime lastAskedAt,
        double averageSimilarity,
        double similarityThreshold
) {}
