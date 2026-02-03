# Requirements Assistant AI

API para gerenciamento e refinamento de requisitos de software com IA (Gemini, OpenAI ou Ollama), chatbot RAG e login via Google.

## Pré-requisitos

- Java 17+
- Docker (PostgreSQL + PGVector)
- API key do provedor de IA escolhido (Gemini ou OpenAI), ou Ollama rodando localmente

## Execução rápida

```bash
# 1. Subir o banco de dados
docker-compose up -d

# 2. Configurar variáveis (copiar example e preencher)
cp application.properties.example application.properties
# Editar application.properties com suas chaves e senhas

# 3. Rodar a aplicação
./mvnw spring-boot:run
```

A API estará em `http://localhost:8080`.

## Variáveis de ambiente (.env ou application.properties)

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `JWT_SECRET` | Sim | Segredo para JWT (mín. 32 caracteres) |
| `GEMINI_API_KEY` | Se ai.provider=gemini | API key do Google AI |
| `OPENAI_API_KEY` | Se ai.provider=openai | API key da OpenAI |
| `GOOGLE_CLIENT_ID` | Opcional | Client ID do Google OAuth (valida aud do idToken) |
| `PGVECTOR_PASSWORD` | Opcional | Senha do PGVector (default: mesma do datasource) |

Copie `.env.example` para `.env` e preencha.

## Papéis (Admin e User)

- **Admin** – Gerencia requirements, requirement-sets, assistente e chatbot. Login: `POST /api/auth/admin/google`.
- **User** – Acessa chatbot dentro do horário configurado. Login: `POST /api/auth/user/google`.

Emails de admin são definidos em `auth.admin.emails` (separados por vírgula).

## Endpoints principais

- `POST /api/auth/user/google` – Login user
- `POST /api/auth/admin/google` – Login admin
- `POST /api/requirement-sets` – Criar projeto (admin)
- `POST /api/requirements-assistant` – Processar requisito com IA (admin)
- `POST /api/requirements/{id}/approve` – Aprovar e salvar no RAG (admin)
- `POST /api/admin/chatbot/config` – Configurar chatbot (admin)
- `POST /api/chatbot/ask` – Perguntar ao chatbot (user/admin)

## Testes

Use o arquivo `http-requests.http` (VS Code REST Client ou JetBrains HTTP Client) para testar os endpoints.
