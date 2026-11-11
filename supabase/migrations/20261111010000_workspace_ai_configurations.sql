-- Credenciais BYOK para a LLM de chat e análise de requisitos.
-- A chave é cifrada pela aplicação (AES-256-GCM); nunca armazene chaves em texto puro.
CREATE TABLE IF NOT EXISTS workspace_ai_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL UNIQUE REFERENCES workspaces(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GEMINI', 'OPENAI', 'ANTHROPIC')),
    model VARCHAR(120) NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    encryption_iv VARCHAR(32) NOT NULL,
    key_hint VARCHAR(16) NOT NULL,
    key_version VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
