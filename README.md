# Requirements Assistant AI

API para gerenciamento e refinamento de requisitos de software com IA (Gemini), chatbot RAG educacional e login via Google.

## Pré-requisitos

- Java 17+
- Docker (PostgreSQL + PGVector)
- API key do Google Gemini AI

## Execução rápida

### Opção 1: Docker Compose (Recomendado)

```bash
# 1. Subir banco de dados e aplicação
docker-compose up -d

# 2. Configurar variáveis de ambiente no docker-compose.yml
# Edite as variáveis GEMINI_API_KEY, AUTH_JWT_SECRET, etc.
```

### Opção 2: Desenvolvimento Local

```bash
# 1. Subir apenas o banco de dados
docker-compose up -d postgres

# 2. Configurar variáveis (copiar example e preencher)
cp application.properties.example application.properties
# Editar application.properties com suas chaves e senhas

# 3. Rodar a aplicação
./mvnw spring-boot:run
```

A API estará em `http://localhost:8080`.

**Nota**: Para mais detalhes sobre Docker, consulte [README_DOCKER.md](README_DOCKER.md).

## Documentação Swagger

Após iniciar a aplicação, acesse:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Configuração (application.properties)

| Propriedade | Obrigatória | Descrição |
|-------------|-------------|-----------|
| `gemini.api-key` | Sim | API key do Google Gemini AI |
| `gemini.model` | Não | Modelo Gemini (padrão: gemini-2.5-flash) |
| `spring.datasource.url` | Sim | URL do PostgreSQL |
| `spring.datasource.username` | Sim | Usuário do banco |
| `spring.datasource.password` | Sim | Senha do banco |
| `auth.jwt.secret` | Sim | Segredo para JWT (mín. 32 caracteres) |
| `auth.admin.emails` | Não | Emails de admin (separados por vírgula) |
| `auth.google.client-id` | Opcional | Client ID do Google OAuth |

## Autenticação

### Login via Google OAuth
- `POST /api/auth/user/google` – Login como usuário
- `POST /api/auth/admin/google` – Login como admin

### Tokens de Teste (Desenvolvimento)
Para facilitar testes locais, use:
- **Token**: `test-admin` → Autentica como admin
- **Token**: `id-user` → Autentica como usuário normal

Envie o token no campo `idToken` do body da requisição.

## Papéis (Admin e User)

- **Admin** – Gerencia requirements, requirement-sets, assistente e chatbot. Pode aprovar requisitos e configurar o chatbot.
- **User** – Acessa chatbot dentro do horário configurado. O chatbot é educacional e não fornece código ou implementações prontas.

Emails de admin são definidos em `auth.admin.emails` (separados por vírgula).

## Endpoints principais

### Autenticação
- `POST /api/auth/user/google` – Login user
- `POST /api/auth/admin/google` – Login admin

### Requisitos (Admin)
- `POST /api/requirement-sets` – Criar projeto
- `POST /api/requirements` – Processar requisito com IA
- `GET /api/requirements` – Listar requisitos
- `PUT /api/requirements/{id}` – Atualizar requisito pendente
- `POST /api/requirements/{id}/approve` – Aprovar e salvar no RAG
- `DELETE /api/requirements/{id}` – Deletar requisito

### Chatbot (User/Admin)
- `POST /api/admin/chatbot/config` – Configurar chatbot (admin)
- `POST /api/chatbot/ask` – Perguntar ao chatbot (user/admin)

**Nota**: O chatbot é educacional e orienta os alunos sobre requisitos, mas **não fornece código, schemas SQL ou implementações prontas**. O objetivo é que os alunos aprendam criando suas próprias soluções.

## Funcionalidades

- **Refinamento de Requisitos com IA**: Processa requisitos brutos usando Gemini AI, seguindo padrão INVEST
- **Detecção de Conflitos**: Identifica requisitos duplicados ou similares
- **Chatbot RAG Educacional**: Responde perguntas sobre requisitos aprovados usando RAG (sem fornecer código)
- **Análise e Classificação de Requisitos**: Classifica requisitos em FR, Security NFR ou Reliability NFR usando few-shot learning
- **Gerenciamento de Projetos**: Organiza requisitos em requirement-sets
- **Histórico de Requisitos**: Mantém histórico de todas as alterações


