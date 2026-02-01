package requirementsAssistantAI.requirementsAssistantAI.application.ports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Porta (interface) do serviço de IA para o chatbot RAG.
 * Responde perguntas sobre requisitos aprovados com base no contexto fornecido.
 */
public interface ChatAiService {

    @SystemMessage("""
        Você é um assistente especializado em responder dúvidas sobre requisitos de software.
        
        CONTEXTO DOS REQUISITOS APROVADOS:
        {{contexto}}
        
        SUA TAREFA:
        1. Responda perguntas dos usuários sobre a lógica e funcionamento dos requisitos.
        2. Use APENAS as informações do contexto fornecido acima.
        3. Se a pergunta não estiver relacionada aos requisitos, informe educadamente.
        4. Seja claro, objetivo e use exemplos quando apropriado.
        5. Se não souber a resposta baseado no contexto, diga que não tem essa informação nos requisitos.
        
        IMPORTANTE: Baseie suas respostas APENAS nos requisitos aprovados fornecidos no contexto.
        """)
    String answerQuestion(@UserMessage String question, @V("contexto") String context);
}
