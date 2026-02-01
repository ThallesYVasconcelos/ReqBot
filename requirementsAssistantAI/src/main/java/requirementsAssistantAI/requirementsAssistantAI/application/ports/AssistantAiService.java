package requirementsAssistantAI.requirementsAssistantAI.application.ports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Porta (interface) do serviço de IA para refinamento de requisitos.
 * A implementação é gerada pelo LangChain4j via AiServices.
 */
public interface AssistantAiService {

    /**
     * Refina um requisito bruto seguindo o padrão INVEST, considerando o contexto de requisitos aprovados.
     */
    @SystemMessage("""
        Você é um Arquiteto de Software Sênior e Especialista em Requisitos.
        
        CONTEXTO DO PROJETO (Requisitos Já Aprovados Relacionados):
        {{contexto}}
        
        SUA TAREFA:
        1. Analise o novo requisito do usuário.
        2. VERIFIQUE SE HÁ CONFLITOS com o 'Contexto do Projeto' acima.
           - Se o novo requisito contradizer algo aprovado, avise explicitamente.
        3. Se não houver conflito, refine o requisito usando o padrão INVEST.
        4. Quando apropriado, use a ferramenta estimateStoryPoints para indicar complexidade (BAIXA, MEDIA, ALTA, MUITO_ALTA).
        
        FORMATO DE SAÍDA:
        [ID] - [TITULO]
        
        **Status de Validação:** (OK ou CONFLITO DETECTADO)
        
        **Análise:**
        (Sua análise sobre qualidade e conflitos)
        
        **Requisito Refinado:**
        (User Story e Critérios de Aceite)
        """)
    String refineRequirement(@UserMessage String rawRequirement, @V("contexto") String context);
}
