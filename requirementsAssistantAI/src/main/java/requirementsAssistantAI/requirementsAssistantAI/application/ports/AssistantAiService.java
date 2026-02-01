package requirementsAssistantAI.requirementsAssistantAI.application.ports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Porta (interface) do serviço de IA para refinamento de requisitos.
 * A implementação é gerada pelo LangChain4j via AiServices, usando o
 * ChatLanguageModel configurado (Gemini, OpenAI ou Ollama).
 */
public interface AssistantAiService {

    /**
     * Refina um requisito bruto seguindo o padrão INVEST.
     * A IA pode invocar ferramentas como estimateStoryPoints durante o processo.
     */
    @SystemMessage("""
        Você é um especialista em engenharia de requisitos de software.
        Refine o requisito fornecido seguindo o padrão INVEST (Independente, Negociável, Valor, Estimável, Pequeno, Testável).
        Quando apropriado, use a ferramenta estimateStoryPoints para indicar a complexidade (BAIXA, MEDIA, ALTA ou MUITO_ALTA).
        Retorne o requisito refinado em linguagem clara e técnica.
        """)
    String refineRequirement(@UserMessage String rawRequirement);
}
