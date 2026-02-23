-- =============================================================================
-- Script SQL para criar todas as tabelas do Requirements Assistant AI no Supabase
-- Execute no SQL Editor do Supabase (NÃO no Logs & Analytics, que só aceita SELECT)
-- Dashboard > SQL Editor > New query > Cole este script > Run
-- =============================================================================

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

-- 3. Tabela requirement_sets (sem dependências)
CREATE TABLE IF NOT EXISTS requirement_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 4. Tabela requirements (depende de requirement_sets)
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

-- 5. Tabela requirement_history (depende de requirements)
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

-- 6. Tabela chatbot_config (depende de requirement_sets)
CREATE TABLE IF NOT EXISTS chatbot_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    requirement_set_id UUID NOT NULL REFERENCES requirement_sets(id) ON DELETE CASCADE,
    start_time TIME,
    end_time TIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chatbot_config_requirement_set_id ON chatbot_config(requirement_set_id);

-- 7. Tabela embeddings (LangChain4j PGVector - AllMiniLmL6V2 usa dimensão 384)
CREATE TABLE IF NOT EXISTS embeddings (
    embedding_id UUID PRIMARY KEY,
    embedding vector(384),
    text TEXT,
    metadata JSON
);
