-- Repara esquemas antigos: tabelas criadas sem workspace (Hibernate/versão anterior).
-- Requer que a tabela "workspaces" JÁ EXISTA. Se o erro for
--   relation "workspaces" does not exist
-- não uses este ficheiro: aplica antes supabase/one_off_fresh_start.sql (apaga dados).

-- requirement_sets: associação ao workspace
ALTER TABLE requirement_sets
  ADD COLUMN IF NOT EXISTS workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_requirement_sets_workspace_id ON requirement_sets(workspace_id);

-- chat_messages: analytics por workspace
ALTER TABLE chat_messages
  ADD COLUMN IF NOT EXISTS workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_chat_messages_workspace_id ON chat_messages(workspace_id);
