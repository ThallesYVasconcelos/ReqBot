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

### Deploy em plataformas cloud

A aplicação é um contêiner Java/Spring Boot padrão e pode ser implantada em qualquer plataforma que suporte Docker:

| Plataforma | Observações |
|------------|-------------|
| **AWS** | ECS, EKS, App Runner, Elastic Beanstalk |
| **Azure** | Container Apps, App Service, AKS |
| **Google Cloud** | Cloud Run, GKE |
| **Kubernetes** | Qualquer cluster (K8s, OpenShift, etc.) |
| **VPS / VM** | Docker ou `java -jar` + systemd |

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

**Importante:** `SPRING_DATASOURCE_URL` deve começar com `jdbc:postgresql://`, não apenas `postgresql://`.

### Deploy no Google Cloud Run

O projeto inclui workflow de CI/CD para Cloud Run:

- **GitHub Actions:** push em `main` dispara deploy em `.github/workflows/deploy.yml`

Consulte a seção de deploy no README ou os comentários no workflow para configurar os secrets do GitHub.

### Solução de problemas: OAuth, CORS, Supabase e 403

| Erro | Causa | Solução |
|------|-------|---------|
| **CORS** "No Access-Control-Allow-Origin" | Backend não permite a origem do frontend | Secret `CORS_ALLOWED_ORIGINS` com `http://localhost:4200,https://reqbot-teal.vercel.app` (vírgula ou `;`). Redeploy. |
| **GSI_LOGGER** "origin not allowed" | Origem não cadastrada no Google | Google Console → Credenciais → OAuth → Origens JavaScript: adicione `http://localhost:4200`, `http://localhost`, `https://reqbot-teal.vercel.app`, `https://requirements-assistant-teal.vercel.app`. Aguarde 5–30 min. |
| **403 Forbidden** no login | Email não autorizado | Admin: email em `AUTH_ADMIN_EMAILS` ou `@computacao.ufcg.edu.br` / `@dsc.ufcg.edu.br`. User: `@ccc.ufcg.edu.br`. |
| **500** no login | Erro no backend (token, banco) | Verifique logs do Cloud Run. Confirme que `AUTH_GOOGLE_CLIENT_ID` é o mesmo do frontend. |
| **Container failed to start** / **MaxClientsInSessionMode** | Supabase Session mode (porta 5432) esgota conexões no Cloud Run | Use **Transaction mode** (porta 6543). Veja [Supabase + Cloud Run](#supabase--cloud-run) abaixo. |

#### Supabase + Cloud Run

Se o backend falha ao iniciar com `MaxClientsInSessionMode: max clients reached` ou `Container failed to start and listen on PORT`:

1. **Use Transaction mode** em vez de Session mode. No Supabase Dashboard → Connect → escolha **Transaction** (porta 6543).
2. **Atualize o secret `SPRING_DATASOURCE_URL`** no GitHub para a URL do Transaction mode:
   ```
   jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
   ```
   - Troque `aws-1-us-east-1` pelo host do seu projeto (ex: `aws-0-us-east-1`).
   - O parâmetro `prepareThreshold=0` é obrigatório para Transaction mode.
3. O profile `cloud` (já ativado no deploy) reduz o pool de conexões para 3, evitando esgotar o limite do Supabase.

#### OAuth no frontend (Vercel e localhost)

- O **frontend** deve usar o **mesmo Client ID** configurado no backend (`AUTH_GOOGLE_CLIENT_ID`).
- No Google Cloud Console → Credenciais → seu cliente OAuth:
  - **Origens JavaScript autorizadas:** `http://localhost:4200`, `https://reqbot-teal.vercel.app`, `https://requirements-assistant-teal.vercel.app`
  - **URIs de redirecionamento:** os mesmos domínios (o frontend usa One Tap / popup, mas alguns fluxos exigem redirect).
- Se o login retorna 500, o backend provavelmente não está respondendo — verifique se o Cloud Run está saudável e se a URL do backend no frontend está correta.

#### Backend via ngrok (URL pública temporária)

Quando o backend está exposto via ngrok (ex: `https://xxxx.ngrok-free.dev`):

1. **Google Cloud Console** → Credenciais → seu cliente OAuth:
   - **Origens JavaScript autorizadas:** adicione `https://SEU-DOMINIO.ngrok-free.dev`
   - **URIs de redirecionamento:** adicione `https://SEU-DOMINIO.ngrok-free.dev` (e `https://SEU-DOMINIO.ngrok-free.dev/api/auth/callback/google` se usar fluxo redirect)

2. **CORS:** inclua a URL do ngrok em `CORS_ALLOWED_ORIGINS` no `docker-compose.yml` ou `.env`.

3. **Header ngrok-skip-browser-warning:** o ngrok exibe uma página de aviso na primeira visita. Para evitar que as requisições fiquem presas, o **frontend** deve enviar o header `ngrok-skip-browser-warning: 69420` em **todas** as chamadas à API quando a URL base for ngrok. Exemplo (Angular HttpClient):
   ```typescript
   // No interceptor ou no serviço de API
   headers = headers.set('ngrok-skip-browser-warning', '69420');
   ```

---

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
