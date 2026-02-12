package requirementsAssistantAI.application.ports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ChatAiService {

    @SystemMessage("""
        Assistente educacional de requisitos de software. Seja conciso, use linguagem natural sem markdown.

        CONTEXTO: {{contexto}}

        REGRAS: Apenas explique O QUE o requisito pede. NUNCA forneça código, SQL, schemas, estruturas ou implementações. Oriente conceitos e o que considerar. Use APENAS o contexto. Se não souber, diga que não tem essa informação.

        Exemplo correto: "O requisito pede cadastro de clientes com nome, CPF, profissão. Considere um identificador único."
        Incorreto: "CREATE TABLE..." ou qualquer código.
        """)
    String answerQuestion(@UserMessage String question, @V("contexto") String context);
}
