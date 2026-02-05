package requirementsAssistantAI.infrastructure.config;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Ferramentas que a IA pode invocar durante o refinamento.
 * Localizada na camada de Infrastructure.
 */
@Component
public class AssistantTools {

    /**
     * Auxilia a IA a cumprir o critério "Estimável" do padrão INVEST.
     * A IA identifica a complexidade e o Java define o valor numérico.
     */
    @Tool("Calcula a estimativa de Story Points (Fibonacci) baseada no nível de complexidade técnica.")
    public int estimateStoryPoints(String complexityLevel) {
        if (complexityLevel == null) return 1;

        // Centralizamos a regra de negócio: a IA decide a categoria, o código define o ponto.
        return switch (complexityLevel.toUpperCase()) {
            case "BAIXA" -> 2;
            case "MEDIA" -> 5;
            case "ALTA" -> 8;
            case "MUITO_ALTA" -> 13;
            default -> 1;
        };
    }

    // O método generateRequirementId foi removido daqui pois agora
    // é uma operação de banco de dados executada apenas na Aprovação.
}
