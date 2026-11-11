# ReqBot

API Spring Boot para gestão de requisitos e chatbots RAG com Gemini, PostgreSQL/Supabase e PGVector.

## Tecnologias

- Java 17 e Spring Boot 4
- PostgreSQL 17 com PGVector
- Gemini Chat e `gemini-embedding-001`
- Google OAuth e JWT
- Docker, Supabase e Render

## Rodando localmente

Suba apenas o banco local:

```bash
docker compose -f docker/docker-compose.yml up -d postgres
cp application.properties.example application.properties
mvn spring-boot:run
```

Ou suba aplicação + banco em Docker:

```bash
cp docker/.env.example docker/.env
docker compose -f docker/docker-compose.yml up -d
```

Health check local:

```bash
curl http://localhost:8080/api/health
```

## Variáveis obrigatórias

No Render, configure estas variáveis no serviço web:

```env
SPRING_PROFILES_ACTIVE=prod
GEMINI_API_KEY=
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
SPRING_DATASOURCE_USERNAME=postgres.SEUPROJECTREF
SPRING_DATASOURCE_PASSWORD=
AUTH_JWT_SECRET=
AUTH_GOOGLE_CLIENT_ID=
CORS_ALLOWED_ORIGINS=
AI_CREDENTIALS_ENCRYPTION_KEY=
```

Notas importantes:

- `PORT` não precisa ser criado manualmente no Render. A plataforma fornece essa variável e o app usa `server.port=${PORT:8080}`.
- `SPRING_PROFILES_ACTIVE=prod` ativa `application-prod.yml`, que reduz conexões, threads e memória para produção.
- `GEMINI_API_KEY` é obrigatória para chat e embeddings. Pegue em <https://aistudio.google.com/apikey>.
- `SPRING_DATASOURCE_URL` deve usar o Transaction Pooler do Supabase, porta `6543`, com `prepareThreshold=0`.
- `SPRING_DATASOURCE_USERNAME` no Supabase pooler normalmente é `postgres.PROJECT_REF`, por exemplo `postgres.qujyvhwiubaxvposstxi`.
- `SPRING_DATASOURCE_PASSWORD` é a senha do banco Supabase.
- `AUTH_JWT_SECRET` deve ter no mínimo 32 caracteres aleatórios.
- `AUTH_GOOGLE_CLIENT_ID` vem do Google Cloud Console, no OAuth Client ID do frontend.
- `CORS_ALLOWED_ORIGINS` deve conter a URL do seu frontend, por exemplo `https://seu-frontend.onrender.com`. Separe múltiplas origens por vírgula.
- `AI_CREDENTIALS_ENCRYPTION_KEY` deve ser uma chave aleatória de 32 bytes em Base64 (`openssl rand -base64 32`). Ela cifra as chaves BYOK de cada workspace e não pode ser perdida.
- Os embeddings seguem usando a `GEMINI_API_KEY` global. As chaves próprias dos workspaces alimentam apenas chat e análise de requisitos.

## Deploy no Render

Este projeto já está preparado para Render com `render.yaml` e `Dockerfile`.

### Opção recomendada: Blueprint

1. Faça push do repositório para o GitHub.
2. No Render, clique em **New > Blueprint**.
3. Conecte este repositório.
4. Confirme o blueprint detectado em `render.yaml`.
5. Preencha as variáveis marcadas como secret.
6. Crie o serviço.
7. Aguarde o build e o health check `/api/health`.

### Opção manual: Web Service

1. No Render, clique em **New > Web Service**.
2. Escolha este repositório.
3. Em **Runtime**, escolha **Docker**.
4. Em **Dockerfile Path**, use:

```text
./Dockerfile
```

5. Em **Health Check Path**, use:

```text
/api/health
```

6. Configure as variáveis de ambiente listadas acima.
7. Clique em **Create Web Service**.

## Como saber se deu certo

Depois do deploy, abra:

```text
https://SUA-URL-DO-RENDER/api/health
```

Resposta esperada:

```json
{
  "status": "UP",
  "service": "requirements-assistant"
}
```

Também teste:

```text
https://SUA-URL-DO-RENDER/swagger-ui.html
```

Se `/api/health` responder, a aplicação subiu. Se o Swagger abrir, a API está servindo rotas HTTP corretamente.

## Onde achar cada valor

### Gemini

Google AI Studio:

```text
https://aistudio.google.com/apikey
```

Crie uma API key e coloque em `GEMINI_API_KEY`.

### Supabase

No painel do Supabase:

1. Abra seu projeto.
2. Vá em **Project Settings > Database**.
3. Procure **Connection string**.
4. Use a opção de pooler/transaction pooler.

Formato esperado:

```text
jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
```

Usuário esperado:

```text
postgres.SEUPROJECTREF
```

### Google OAuth

No Google Cloud Console:

1. Vá em **APIs & Services > Credentials**.
2. Abra ou crie um **OAuth 2.0 Client ID**.
3. Copie o **Client ID**.
4. Coloque em `AUTH_GOOGLE_CLIENT_ID`.

## GitHub Secrets x Render

Secrets do GitHub não são usados automaticamente pelo Render.

Eles só influenciam o deploy se um workflow do GitHub Actions mandar esses valores para algum lugar. O workflow atual deste projeto apenas roda testes (`mvn clean test`). Portanto, para o backend no Render, as variáveis que importam são as configuradas no painel do Render.

## Endpoints principais

- Health: `GET /api/health`
- Swagger: `/swagger-ui.html`
- OpenAPI: `/v3/api-docs`
- Login Google: `POST /api/auth/google`
- Workspaces: `/api/workspaces`
- Requisitos: `/api/requirements`
- Chatbots: `/api/chatbots`
- IA por workspace: `GET|PUT|DELETE /api/workspaces/{workspaceId}/ai-configuration` (somente OWNER)
- Modelos suportados: `GET /api/workspaces/{workspaceId}/ai-configuration/models`

### Configuração de IA pelo frontend

O frontend deve enviar a chave somente por HTTPS para o endpoint abaixo, com o JWT do owner:

```http
PUT /api/workspaces/{workspaceId}/ai-configuration
Authorization: Bearer <token>
Content-Type: application/json

{
  "provider": "ANTHROPIC",
  "model": "claude-sonnet-4-5",
  "apiKey": "sk-ant-..."
}
```

Os provedores aceitos são `GEMINI`, `OPENAI` e `ANTHROPIC`. A lista para preencher o seletor de modelo vem de `GET /api/workspaces/{workspaceId}/ai-configuration/models`, no formato:

```json
{
  "OPENAI": ["gpt-4o-mini", "gpt-4o"],
  "GEMINI": ["gemini-2.5-flash"],
  "ANTHROPIC": ["claude-sonnet-4-5"]
}
```

A resposta de configuração nunca contém a chave: apenas provedor, modelo e os quatro últimos caracteres mascarados.

## Testes

```bash
mvn clean test
```
