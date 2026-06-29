# ReqBot

API Spring Boot para gestão de requisitos e chatbots RAG com Gemini, PostgreSQL e PGVector.

## Modelo de acesso

- Toda identidade possui papel global `USER`.
- `OWNER` e `ADMIN` são papéis vinculados a um workspace.
- O `OWNER` cria o workspace, convida e remove administradores.
- `OWNER` e `ADMIN` criam projetos, requisitos e múltiplos chatbots ativos.
- Um usuário final não pertence ao workspace: ele entra em um chatbot usando o código recebido.
- Desativar um chatbot bloqueia o uso do código e as novas perguntas sem apagar associações.

## Arquitetura do RAG

1. Requisitos são persistidos no PostgreSQL.
2. Após o commit, uma fila limitada a uma thread gera ou atualiza o embedding.
3. A busca textual do PostgreSQL é executada primeiro.
4. A API de embeddings é chamada somente quando os resultados literais não forem suficientes.
5. Chatbots do mesmo projeto compartilham requisitos, índice vetorial e cache de respostas.

Os relatórios de similaridade reutilizam vetores já armazenados. O ranking de perguntas usa
similaridade lexical local e não gera um embedding para cada mensagem.

## Tecnologias

- Java 17 e Spring Boot 4
- PostgreSQL 17 e PGVector
- Gemini Chat e `gemini-embedding-001`
- Google OAuth e JWT
- Docker, Supabase e Render

## Execução local

```bash
docker compose -f docker/docker-compose.yml up -d postgres
cp application.properties.example application.properties
mvn spring-boot:run
```

A API local usa `http://localhost:8081` quando configurada pelo arquivo local.

Para executar tudo em Docker:

```bash
cp docker/.env.example docker/.env
docker compose -f docker/docker-compose.yml up -d
```

## Configuração obrigatória

Não versione secrets. Use propriedades locais ignoradas pelo Git ou variáveis de ambiente:

```text
GEMINI_API_KEY=
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:6543/postgres?sslmode=require&prepareThreshold=0
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
AUTH_JWT_SECRET=
AUTH_GOOGLE_CLIENT_ID=
CORS_ALLOWED_ORIGINS=
```

Para o Supabase, prefira o Transaction Pooler e mantenha `prepareThreshold=0` na URL JDBC.

## Endpoints principais

### Autenticação

- `POST /api/auth/google`

### Workspaces e administradores

- `POST /api/workspaces`
- `GET /api/workspaces`
- `GET /api/workspaces/{workspaceId}`
- `POST /api/workspaces/{workspaceId}/admin-invitations`
- `POST /api/workspace-admin-invitations/accept`
- `DELETE /api/workspaces/{workspaceId}/admins/{adminUserId}`

### Projetos e requisitos

- `POST /api/workspaces/{workspaceId}/requirement-sets`
- `GET /api/workspaces/{workspaceId}/requirement-sets`
- `POST /api/requirements/refine`
- `POST /api/requirements/save`
- `GET /api/requirements?requirementSetId={projectId}`
- `GET /api/requirements/report?requirementSetId={projectId}`

### Chatbots

- `POST /api/workspaces/{workspaceId}/chatbots`
- `GET /api/workspaces/{workspaceId}/chatbots`
- `PATCH /api/workspaces/{workspaceId}/chatbots/{chatbotId}/active?active=false`
- `POST /api/chatbots/join`
- `GET /api/chatbots/me`
- `POST /api/chatbots/{chatbotId}/chat/ask`
- `GET /api/chatbots/{chatbotId}/chat/history/me`

Swagger: `/swagger-ui.html` · OpenAPI: `/v3/api-docs` · Health: `/api/health`

## Testes

```bash
mvn clean test
```

## Deploy gratuito no Render

O arquivo `render.yaml` define um web service Docker gratuito e o perfil
`application-prod.yml` limita conexões, threads, históricos, cache e fila de embeddings.

1. No Render, escolha **New > Blueprint** e conecte este repositório.
2. Preencha os secrets solicitados pelo blueprint.
3. Use a URL do Transaction Pooler do Supabase em `SPRING_DATASOURCE_URL`.
4. Inclua a origem do frontend em `CORS_ALLOWED_ORIGINS`.
5. Crie o serviço e aguarde o health check `/api/health` ficar saudável.

Serviços gratuitos do Render hibernam após 15 minutos sem tráfego e compartilham a cota
mensal de 750 horas. O primeiro acesso após a hibernação terá cold start.

O blueprint usa `checksPass`: o Render só inicia o deploy depois que o workflow de CI passar.

Documentação oficial: [Render Free](https://render.com/docs/free).
