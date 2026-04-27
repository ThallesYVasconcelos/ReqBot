-- =============================================================================
-- ReqBot — RESET total + esquema novo (use no SQL Editor do Supabase)
-- ATENÇÃO: apaga TODAS as tabelas listadas abaixo e os dados. Irreversível.
-- Use quando os dados antigos não importam e a base está inconsistente
-- (ex.: sem tabela "workspaces", mas com tabelas antigas do app).
-- =============================================================================

-- Ordem: dependentes primeiro (FKs)
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS chatbot_config CASCADE;
DROP TABLE IF EXISTS requirement_history CASCADE;
DROP TABLE IF EXISTS requirements CASCADE;
DROP TABLE IF EXISTS requirement_sets CASCADE;
DROP TABLE IF EXISTS workspace_members CASCADE;
DROP TABLE IF EXISTS embeddings CASCADE;
DROP TABLE IF EXISTS workspaces CASCADE;
DROP TABLE IF EXISTS app_users CASCADE;

-- ---------------------------------------------------------------------------
-- Esquema completo (igual a migrations/20250425120000_initial_schema.sql)
-- ---------------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    picture_url VARCHAR(500),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL CHECK (type IN ('PROFESSIONAL', 'ACADEMIC')),
    owner_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_workspaces_owner_email ON workspaces(owner_email);

CREATE TABLE workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, user_email)
);

CREATE INDEX idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user_email ON workspace_members(user_email);

CREATE TABLE requirement_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_requirement_sets_workspace_id ON requirement_sets(workspace_id);

CREATE TABLE requirements (
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

CREATE INDEX idx_requirements_requirement_set_id ON requirements(requirement_set_id);

CREATE TABLE requirement_history (
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

CREATE INDEX idx_requirement_history_requirement_uuid ON requirement_history(requirement_uuid);

CREATE TABLE chatbot_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    requirement_set_id UUID NOT NULL REFERENCES requirement_sets(id) ON DELETE CASCADE,
    start_time TIME,
    end_time TIME,
    show_requirements_to_users BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_chatbot_config_requirement_set_id ON chatbot_config(requirement_set_id);

CREATE TABLE embeddings (
    embedding_id UUID PRIMARY KEY,
    embedding vector(768),
    text TEXT,
    metadata JSON
);

CREATE TABLE chat_messages (
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

CREATE INDEX idx_chat_messages_requirement_set_id ON chat_messages(requirement_set_id);
CREATE INDEX idx_chat_messages_workspace_id ON chat_messages(workspace_id);
CREATE INDEX idx_chat_messages_user_email ON chat_messages(user_email);
CREATE INDEX idx_chat_messages_asked_at ON chat_messages(asked_at DESC);
