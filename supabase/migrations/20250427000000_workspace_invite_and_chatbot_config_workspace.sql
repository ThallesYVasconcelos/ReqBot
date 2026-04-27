-- Migration: invite_code no workspace + workspace_id no chatbot_config

-- 1. invite_code na tabela workspaces
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS invite_code VARCHAR(16) UNIQUE;

-- Gera códigos para workspaces existentes que ainda não possuem um
UPDATE workspaces
SET invite_code = UPPER(SUBSTRING(MD5(id::text || RANDOM()::text), 1, 8))
WHERE invite_code IS NULL;

-- 2. workspace_id na tabela chatbot_config (referencia o workspace do requirement_set)
ALTER TABLE chatbot_config
    ADD COLUMN IF NOT EXISTS workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL;

-- Preenche workspace_id para registros existentes usando o workspace do requirement_set
UPDATE chatbot_config cc
SET workspace_id = rs.workspace_id
FROM requirement_sets rs
WHERE cc.requirement_set_id = rs.id
  AND cc.workspace_id IS NULL
  AND rs.workspace_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_chatbot_config_workspace_id ON chatbot_config(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspaces_invite_code ON workspaces(invite_code);
