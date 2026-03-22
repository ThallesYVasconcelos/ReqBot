# ReqBot - Requirements Assistant AI

API para gerenciamento e refinamento de requisitos de software com IA (Gemini), chatbot RAG educacional e autenticação via Google OAuth.

## Tecnologias

- **Java 17** · Spring Boot 4 · Spring Security
- **PostgreSQL** · PGVector (embeddings)
- **Gemini AI** · LangChain4j
- **Google OAuth** · JWT

## Pré-requisitos

- Java 17+
- Maven 3.8+
- Docker e Docker Compose
- API key do [Google Gemini AI](https://aistudio.google.com/apikey)
- Credenciais Google OAuth (para login)

## Execução

### Docker (recomendado)

```bash
# 1. Criar .env com suas chaves (obrigatório para IA e login)
cp docker/.env.example docker/.env
# Edite docker/.env com GEMINI_API_KEY, AUTH_JWT_SECRET, etc.

# 2. Subir banco e aplicação
docker compose -f docker/docker-compose.yml up -d

# Ver logs (útil para debug)
docker compose -f docker/docker-compose.yml logs -f app
```

**Portas:** API em `http://localhost:8080` · PostgreSQL em `localhost:5433`

**Variáveis de ambiente:** O `application.properties` é excluído do build Docker por segurança. Configure `GEMINI_API_KEY` e `AUTH_JWT_SECRET` em `docker/.env` (copie de `docker/.env.example`).

```bash
# Apenas o banco (para desenvolvimento local)
docker compose -f docker/docker-compose.yml up -d postgres
```

### Desenvolvimento local

```bash
# 1. Subir o banco
docker compose -f docker/docker-compose.yml up -d postgres

# 2. Configurar (copiar e editar)
cp application.properties.example application.properties

# 3. Rodar a aplicação
mvn spring-boot:run
```

**Porta local:** `http://localhost:8081` (configurada em `application.properties`)

## Documentação da API

- **Swagger UI:** `http://localhost:8080/swagger-ui.html` (ou 8081 em dev local)
- **OpenAPI:** `http://localhost:8080/v3/api-docs`

## Configuração

A aplicação pode ser configurada via arquivo `application.properties` ou variáveis de ambiente. Em produção, prefira variáveis de ambiente ou secrets do provedor de cloud.

### Propriedades principais

| Propriedade / Variável de ambiente | Obrigatória | Descrição |
|-----------------------------------|:-----------:|-----------|
| `gemini.api-key` / `GEMINI_API_KEY` | Sim | API key do [Google Gemini AI](https://aistudio.google.com/apikey) |
| `gemini.model` / `GEMINI_MODEL` | Não | Modelo (padrão: gemini-2.5-flash) |
| `spring.datasource.url` / `SPRING_DATASOURCE_URL` | Sim | URL JDBC (ex: `jdbc:postgresql://host:5432/requirements_db`; adicione `?sslmode=require` se o banco exigir SSL) |
| `spring.datasource.username` / `SPRING_DATASOURCE_USERNAME` | Sim | Usuário do banco |
| `spring.datasource.password` / `SPRING_DATASOURCE_PASSWORD` | Sim | Senha do banco |
| `pgvector.host` / `PGVECTOR_HOST` | Sim | Host do PostgreSQL (para embeddings) |
| `pgvector.port` / `PGVECTOR_PORT` | Sim | Porta (padrão: 5432) |
| `pgvector.database` / `PGVECTOR_DATABASE` | Sim | Nome do banco |
| `pgvector.user` / `PGVECTOR_USER` | Sim | Usuário do banco |
| `pgvector.password` / `PGVECTOR_PASSWORD` | Sim | Senha do banco |
| `auth.jwt.secret` / `AUTH_JWT_SECRET` | Sim | Segredo JWT (mín. 32 caracteres) |
| `auth.admin.emails` / `AUTH_ADMIN_EMAILS` | Não | Emails de admin (separados por `;` ou `,`) |
| `auth.google.client-id` / `AUTH_GOOGLE_CLIENT_ID` | Não | Client ID do Google OAuth |
| `cors.allowed-origins` / `CORS_ALLOWED_ORIGINS` | Não | Origens permitidas (separadas por vírgula ou `;`). Ex: `http://localhost:4200,https://reqbot-teal.vercel.app` |
| `server.port` / `PORT` | Não | Porta HTTP (padrão: 8080; Cloud Run usa `PORT`) |

### Banco de dados

O PostgreSQL precisa da extensão **PGVector** para embeddings. Após criar o banco, execute:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Arquivo de exemplo

```bash
cp application.properties.example application.properties
# Edite application.properties com seus valores
```

## Autenticação

### Google OAuth

- `POST /api/auth/user/google` — Login como usuário
- `POST /api/auth/admin/google` — Login como admin

Envie o `idToken` do Google no body da requisição.

### Tokens de teste (desenvolvimento)

- `test-admin` — Autentica como admin
- `id-user` — Autentica como usuário

## Papéis

| Papel | Permissões |
|-------|------------|
| **Admin** | Projetos, requisitos, refinamento com IA, chatbot, relatórios |
| **User** | Chatbot (dentro do horário configurado) |

## Endpoints principais

### Autenticação
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/user/google` | Login usuário |
| POST | `/api/auth/admin/google` | Login admin |

### Projetos (Requirement Sets)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/requirement-sets` | Criar projeto (nome + descrição) |
| GET | `/api/requirement-sets` | Listar projetos |
| GET | `/api/requirement-sets/{id}` | Obter projeto |
| DELETE | `/api/requirement-sets/{id}` | Excluir projeto |
| GET | `/api/requirement-sets/{id}/requirements` | Listar requisitos do projeto |

### Requisitos
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/requirements` | Refinar requisito com IA |
| POST | `/api/requirements/refine` | Refinar (alternativo) |
| POST | `/api/requirements/save` | Salvar requisito refinado |
| GET | `/api/requirements` | Listar (filtro por projeto) |
| GET | `/api/requirements/{id}` | Obter requisito |
| GET | `/api/requirements/set/{id}` | Requisitos por projeto |
| GET | `/api/requirements/report` | Relatório de conflitos/ambiguidades |
| GET | `/api/requirements/{id}/history` | Histórico de alterações |
| PUT | `/api/requirements/{id}` | Atualizar requisito |
| DELETE | `/api/requirements/{id}` | Excluir requisito |

### Chatbot
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/admin/chatbot/config` | Configurar chatbot (admin) |
| GET | `/api/admin/chatbot/config` | Listar configurações |
| POST | `/api/chatbot/ask` | Perguntar ao chatbot |

## Funcionalidades

- **Refinamento com IA** — Processa requisitos brutos com Gemini, seguindo padrão INVEST e classificação PURE (FR, NFR-Security, NFR-Reliability)
- **Pontos de ambiguidade** — Identifica trechos obscuros e sugere melhorias
- **Detecção de conflitos** — Encontra requisitos duplicados ou similares via embeddings
- **Chatbot RAG** — Responde perguntas sobre requisitos salvos (educacional, sem fornecer código)
- **Projetos** — Organiza requisitos em conjuntos com nome e descrição
- **Histórico** — Registra alterações de cada requisito

## Deploy

### Build da imagem Docker

```bash
docker build -f docker/Dockerfile -t requirements-assistant:latest .
```

### Executar com Docker

```bash
docker run -p 8080:8080 \
  -e GEMINI_API_KEY=sua_key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/requirements_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=senha \
  -e PGVECTOR_HOST=host \
  -e PGVECTOR_PORT=5432 \
  -e PGVECTOR_DATABASE=requirements_db \
  -e PGVECTOR_USER=postgres \
  -e PGVECTOR_PASSWORD=senha \
  -e AUTH_JWT_SECRET=segredo_minimo_32_caracteres \
  requirements-assistant:latest
```

### Docker Compose (produção)

Para subir aplicação + banco com `docker-compose`:

```bash
docker compose -f docker/docker-compose.yml up -d
```
**Requisitos gerais:**
- Porta configurável via `PORT` (padrão 8080)
- Memória mínima recomendada: 2Gi (modelo ONNX para embeddings)
- Conectividade com PostgreSQL (banco com PGVector)

### Variáveis de ambiente em produção

Em qualquer ambiente, configure pelo menos:

```
GEMINI_API_KEY=
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
PGVECTOR_HOST=
PGVECTOR_PORT=
PGVECTOR_DATABASE=
PGVECTOR_USER=
PGVECTOR_PASSWORD=
AUTH_JWT_SECRET=
AUTH_ADMIN_EMAILS=email1@exemplo.com;email2@exemplo.com
AUTH_GOOGLE_CLIENT_ID=  # opcional, para OAuth
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://seu-app.vercel.app  # opcional; inclua localhost para testar local contra API em produção
```

## Estrutura do projeto

```
├── .github/workflows/
│   └── deploy.yml          # Deploy automático (GitHub Actions)
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── src/main/java/requirementsAssistantAI/
│   ├── application/     # Controllers, services, ports
│   ├── domain/          # Entidades
│   ├── dto/             # DTOs
│   └── infrastructure/  # Repositories, config
├── pom.xml
└── application.properties.example
```
