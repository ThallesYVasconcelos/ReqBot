-- ReqBot — esquema inicial (PGVector + JPA)
-- Aplique com: Supabase CLI (`supabase db push`) ou SQL Editor (Dashboard > SQL > New query).
-- Mantenha alinhado com docker/init-schema.sql ao evoluir o modelo.

-- 1. Extensão PGVector (necessária para a tabela embeddings)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tabela app_users (sem dependências)
CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    picture_url VARCHAR(500),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 3. Tabela workspaces (raiz multi-tenant)
CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL CHECK (type IN ('PROFESSIONAL', 'ACADEMIC')),
    owner_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspaces_owner_email ON workspaces(owner_email);

-- 4. Tabela workspace_members
CREATE TABLE IF NOT EXISTS workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, user_email)
);

CREATE INDEX IF NOT EXISTS idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_members_user_email ON workspace_members(user_email);

-- 5. Tabela requirement_sets
CREATE TABLE IF NOT EXISTS requirement_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_requirement_sets_workspace_id ON requirement_sets(workspace_id);

-- 6. Tabela requirements
CREATE TABLE IF NOT EXISTS requirements (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id VARCHAR(255),
    refined_requirement TEXT,
    requirement_hash VARCHAR(255),
    raw_requirement TEXT,
    analise TEXT,
    ambiguity_warnings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    requirement_set_id UUID NOT NULL REFERENCES requirement_sets(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_requirements_requirement_set_id ON requirements(requirement_set_id);

-- 7. Tabela requirement_history
CREATE TABLE IF NOT EXISTS requirement_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_uuid UUID NOT NULL REFERENCES requirements(uuid) ON DELETE CASCADE,
    requirement_id VARCHAR(255),
    raw_requirement TEXT,
    refined_requirement TEXT,
    requirement_hash VARCHAR(255),
    analise TEXT,
    ambiguity_warnings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_type VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_requirement_history_requirement_uuid ON requirement_history(requirement_uuid);

-- 8. Tabela chatbot_config
CREATE TABLE IF NOT EXISTS chatbot_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    requirement_set_id UUID NOT NULL REFERENCES requirement_sets(id) ON DELETE CASCADE,
    start_time TIME,
    end_time TIME,
    show_requirements_to_users BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chatbot_config_requirement_set_id ON chatbot_config(requirement_set_id);

-- 9. Tabela embeddings (LangChain4j PGVector — Gemini API, dimensão 768)
CREATE TABLE IF NOT EXISTS embeddings (
    embedding_id UUID PRIMARY KEY,
    embedding vector(768),
    text TEXT,
    metadata JSON
);

-- Índice IVFFlat: descomente após inserir embeddings (recomendado com volume maior)
-- CREATE INDEX IF NOT EXISTS idx_embeddings_vector ON embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 10. Tabela chat_messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email VARCHAR(255),
    question TEXT NOT NULL,
    answer TEXT,
    answered_from_cache BOOLEAN DEFAULT false,
    chatbot_available BOOLEAN DEFAULT true,
    asked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requirement_set_id UUID REFERENCES requirement_sets(id) ON DELETE SET NULL,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_requirement_set_id ON chat_messages(requirement_set_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_workspace_id ON chat_messages(workspace_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_email ON chat_messages(user_email);
CREATE INDEX IF NOT EXISTS idx_chat_messages_asked_at ON chat_messages(asked_at DESC);
