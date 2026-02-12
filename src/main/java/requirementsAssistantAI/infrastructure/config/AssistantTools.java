package requirementsAssistantAI.infrastructure.config;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;


@Component
public class AssistantTools {

   
    @Tool("Calcula a estimativa de Story Points (Fibonacci) baseada no nível de complexidade técnica.")
    public int estimateStoryPoints(String complexityLevel) {
        if (complexityLevel == null) return 1;

        return switch (complexityLevel.toUpperCase()) {
            case "BAIXA" -> 2;
            case "MEDIA" -> 5;
            case "ALTA" -> 8;
            case "MUITO_ALTA" -> 13;
            default -> 1;
        };
    }

}
