# Requirements Assistant AI

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
# Subir banco e aplicação
docker compose -f docker/docker-compose.yml up -d

# Apenas o banco (para desenvolvimento local)
docker compose -f docker/docker-compose.yml up -d postgres
```

**Portas:** API em `http://localhost:8080` · PostgreSQL em `localhost:5433`

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

| Propriedade | Obrigatória | Descrição |
|-------------|:-----------:|-----------|
| `gemini.api-key` | Sim | API key do Google Gemini |
| `gemini.model` | Não | Modelo (padrão: gemini-2.5-flash) |
| `spring.datasource.url` | Sim | URL do PostgreSQL |
| `spring.datasource.username` | Sim | Usuário do banco |
| `spring.datasource.password` | Sim | Senha do banco |
| `auth.jwt.secret` | Sim | Segredo JWT (mín. 32 caracteres) |
| `auth.admin.emails` | Não | Emails de admin (separados por vírgula) |
| `auth.google.client-id` | Não | Client ID do Google OAuth |

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

## Estrutura do projeto

```
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
