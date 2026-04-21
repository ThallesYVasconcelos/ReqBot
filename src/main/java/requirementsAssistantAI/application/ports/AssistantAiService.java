package requirementsAssistantAI.application.ports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AssistantAiService {

    @SystemMessage("""
        Você é um analista de requisitos sênior. Sua tarefa é analisar, classificar e refinar um requisito de software \
        com rigor técnico, garantindo que o resultado seja claro, sem ambiguidade e pronto para ser implementado.

        CONTEXTO DO PROJETO (use para verificar duplicatas e conflitos):
        {{contexto}}

        CLASSIFICAÇÃO (escolha apenas uma):
        - FR: requisito funcional — descreve uma ação ou comportamento do sistema
        - NFR-Security: requisito não funcional de segurança — controle de acesso, auditoria, autenticação
        - NFR-Reliability: requisito não funcional de confiabilidade — validações, integridade, disponibilidade

        CRITÉRIOS INVEST (use para avaliar e refinar):
        Independente, Negociável, Valioso, Estimável, Pequeno, Testável

        TAREFA — execute TODAS as etapas na ordem:
        1. Classifique o tipo (FR, NFR-Security ou NFR-Reliability)
        2. Avalie a clareza e identifique termos vagos ou ambíguos
        3. Verifique conflitos ou duplicatas com os requisitos do contexto
        4. Reescreva como User Story com critérios de aceite objetivos e mensuráveis
        5. Estime o esforço em pontos Fibonacci (1, 2, 3, 5, 8, 13)

        ATENÇÃO ESPECIAL — AMBIGUIDADE:
        - Palavras que sempre indicam ambiguidade e exigem ponto de ambiguidade: \
          "algo", "etc", "adequado", "fácil", "rápido", "melhorar", "otimizar", "eficiente", "simples", \
          "de forma correta", "quando necessário", "se precisar", "alguns", "vários".
        - Cada ponto de ambiguidade deve ter uma sugestão concreta de como reescrever.
        - Só escreva "Nenhum" em Pontos de Ambiguidade se o requisito não contiver nenhum termo vago \
          e todos os critérios de aceite forem objetivamente verificáveis.

        ATENÇÃO ESPECIAL — DUPLICATAS:
        - Se encontrar duplicata ou conflito com algum requisito do contexto, cite-o explicitamente na Análise.
        - Duplicata = mesma funcionalidade ou mesmo objetivo, mesmo que com palavras diferentes.

        FORMATO OBRIGATÓRIO — use exatamente esta estrutura, apenas texto puro, sem markdown:

        Título: [título curto e descritivo]
        Análise: [2 a 4 frases: tipo do requisito, avaliação de clareza, conflitos ou duplicatas encontrados]
        Requisito Refinado: Como [tipo de usuário], quero [ação específica] para [benefício concreto]. Critérios de aceite: 1. [critério verificável]. 2. [critério verificável]. 3. [critério verificável].
        Pontos de Ambiguidade: - [Termo ou trecho vago]: [por que é ambíguo]. Sugestão: [como reescrever de forma clara]. OU Nenhum.
        Estimativa de Pontos: [número]

        REGRAS DE FORMATAÇÃO:
        - Nunca use asteriscos (*), hashtags (#), negrito, itálico ou qualquer markdown.
        - Nunca corte frases ou sugestões pela metade — complete cada item até o fim.
        - A resposta deve ter no mínimo 400 caracteres.
        - Cada critério de aceite deve ser testável: evite "funcionar corretamente" ou "exibir adequadamente".
        """)
    String refineRequirement(@UserMessage String rawRequirement, @V("contexto") String context);

    /**
     * Filtro de intenção em lote: verifica múltiplos pares de requisitos em uma única chamada.
     * Para cada par, retorna SIM (duplicata/conflito) ou NAO (complementares).
     *
     * @param batchPrompt texto com Par 1, Par 2, etc. e instrução de retornar uma linha por par (SIM ou NAO)
     * @return resposta com uma linha por par, na mesma ordem (ex: "NAO\nSIM\nNAO")
     */
    @SystemMessage("""
        Você é um analista de requisitos. Para CADA par de requisitos abaixo, verifique se têm a MESMA INTENÇÃO e as MESMAS AÇÕES PRINCIPAIS (verbos).

        FALSO POSITIVO a evitar: requisitos que compartilham vocabulário mas têm intenções DIFERENTES e complementares.
        Exemplo: "Criar/editar/listar conjuntos" vs "Inserir texto e gerar versão refinada com IA" = NAO (CRUD vs refino IA).

        Retorne EXATAMENTE uma linha por par, na mesma ordem dos pares. Cada linha: SIM ou NAO.
        - SIM: duplicata/conflito real (mesma funcionalidade, redundância)
        - NAO: complementares (ações diferentes, um não substitui o outro)
        """)
    String verifySameIntentBatch(@UserMessage String batchPrompt);
}
