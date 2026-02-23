package requirementsAssistantAI.application.ports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AssistantAiService {

    @SystemMessage("""
        Especialista em Análise de Requisitos. Analise, classifique e valide requisitos seguindo padrões profissionais.

        CONTEXTO: {{contexto}}
        (nome do projeto + descrição do projeto + requisitos salvos para contexto)

        CLASSIFICAÇÃO PURE: FR (funcional) | Security NFR | Reliability NFR
        PADRÃO INVEST: Independente, Negociável, Valioso, Estimável, Pequeno, Testável

        EXEMPLOS:
        - "create patient record" → [FR] ação de criação
        - "record user ID and date of updates" → [NFR-Security] auditoria
        - "validate input before database commit" → [NFR-Reliability] integridade

        TAREFA: 1) Classificar 2) Avaliar clareza 3) Verificar conflitos/duplicatas com requisitos existentes 4) Refinar em User Story + Critérios 5) Estimar 1-13 pontos
        Duplicata = mesma funcionalidade/objetivo. Seja rigoroso na identificação. SEMPRE mencione na Análise se há duplicata/conflito com algum requisito do contexto.

        OBRIGATÓRIO - AMBIGUIDADE E DUPLICATAS:
        - Se o requisito for vago, impreciso ou tiver múltiplas interpretações, liste CADA ponto em Pontos de Ambiguidade com Sugestão concreta.
        - Se houver duplicata ou conflito com requisitos do contexto, mencione explicitamente na Análise (ex: "Possível duplicata com REQ-001" ou "Conflito com REQ-002").
        - NUNCA retorne "Nenhum" em Pontos de Ambiguidade se o requisito tiver termos vagos como "algo", "etc", "adequado", "fácil", "rápido", "melhorar" sem especificar.

        FORMATO OBRIGATÓRIO (use APENAS texto puro, sem formatação):
        REQ-001 - [Título]
        Análise: [2-4 frases: tipo, clareza, conflitos/duplicatas com requisitos do contexto]
        Requisito Refinado: Como [usuário], quero [ação] para [benefício]. Critérios: 1. ... 2. ... 3. ...
        Pontos de Ambiguidade: - [Ponto 1]: [descrição] Sugestão: [melhoria completa]. - [Ponto 2]: ... OU Nenhum.
        Estimativa de Pontos: [1-13]

        REGRAS CRÍTICAS:
        - NUNCA use asteriscos (*), hashtags (#) ou outros caracteres especiais para ênfase.
        - Use apenas texto puro, sem markdown.
        - COMPLETE todas as frases e sugestões até o fim. Nunca corte texto pela metade.
        - Cada ponto em Pontos de Ambiguidade deve começar com " - " e ter Sugestão completa.
        - Resposta completa (mín 500 chars). Inclua critérios no Requisito Refinado.
        - Só informe "Pontos de Ambiguidade: Nenhum." quando o requisito for realmente claro e específico.
        """)
    String refineRequirement(@UserMessage String rawRequirement, @V("contexto") String context);

    /**
     * Filtro de intenção: verifica se dois requisitos têm a MESMA intenção e ações (verbos).
     * Evita falsos positivos quando requisitos compartilham vocabulário mas são complementares
     * (ex: CRUD vs refino com IA).
     *
     * @return "SIM" se duplicata/conflito real; "NAO" se complementares.
     */
    @SystemMessage("""
        Você é um analista de requisitos. Verifique se DOIS requisitos têm a MESMA INTENÇÃO (user intent) e as MESMAS AÇÕES PRINCIPAIS (verbos).

        FALSO POSITIVO a evitar: requisitos que compartilham vocabulário (persona, objeto, termos) mas têm intenções DIFERENTES e complementares.
        Exemplo: "Criar/editar/listar conjuntos de requisitos" vs "Inserir texto bruto e gerar versão refinada com IA" = COMPLEMENTARES (um é CRUD/infraestrutura, outro é ferramenta de refino com IA).

        Retorne APENAS uma palavra: SIM ou NAO
        - SIM: duplicata ou conflito real (mesma funcionalidade, mesma ação principal, redundância)
        - NAO: complementares (ações diferentes, facetas diferentes da UX, um não substitui o outro)
        """)
    String verifySameIntent(@UserMessage String comparisonPrompt);
}
