# Docker Setup

## Build da Imagem

```bash
docker build -t requirements-assistant-ai .
```

## Executar com Docker Compose

### Opção 1: Apenas o Banco de Dados
```bash
docker-compose up -d postgres
```

### Opção 2: Aplicação Completa (Banco + App)
```bash
docker-compose up -d
```

### Variáveis de Ambiente

Para configurar a aplicação, você pode:

1. **Criar um arquivo `.env`** na raiz do projeto:
```env
GEMINI_API_KEY=sua_chave_aqui
AUTH_JWT_SECRET=seu_secret_jwt_aqui_minimo_32_caracteres
AUTH_ADMIN_EMAILS=admin1@example.com,admin2@example.com
```

2. **Atualizar o `docker-compose.yml`** para usar o arquivo `.env`:
```yaml
app:
  env_file:
    - .env
```

3. **Ou passar variáveis diretamente** no `docker-compose.yml` (não recomendado para produção).

## Comandos Úteis

### Ver logs da aplicação
```bash
docker-compose logs -f app
```

### Ver logs do banco
```bash
docker-compose logs -f postgres
```

### Parar todos os serviços
```bash
docker-compose down
```

### Parar e remover volumes (limpar dados)
```bash
docker-compose down -v
```

### Rebuild da aplicação
```bash
docker-compose build app
docker-compose up -d app
```

## Acessar a Aplicação

Após iniciar os serviços:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Banco de Dados**: localhost:5433

## Desenvolvimento Local

Para desenvolvimento, você pode:
1. Executar apenas o banco via Docker: `docker-compose up -d postgres`
2. Executar a aplicação localmente: `./mvnw spring-boot:run`

Isso permite hot-reload e debug mais fácil.
