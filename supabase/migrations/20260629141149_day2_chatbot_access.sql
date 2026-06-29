-- Dia 2: múltiplos chatbots por projeto e ingresso de usuário por código.

begin;

alter table public.chatbot_config
    add column if not exists name varchar(120),
    add column if not exists access_code_hash varchar(64);

update public.chatbot_config chatbot
set name = coalesce(nullif(trim(chatbot.name), ''), project.name || ' - Chatbot')
from public.requirement_sets project
where chatbot.requirement_set_id = project.id
  and (chatbot.name is null or trim(chatbot.name) = '');

-- Códigos legados não são recuperáveis. O hash opaco evita expor ou reutilizar um código.
update public.chatbot_config
set access_code_hash = md5(id::text) || md5(id::text || ':legacy')
where access_code_hash is null;

update public.chatbot_config chatbot
set workspace_id = project.workspace_id
from public.requirement_sets project
where chatbot.requirement_set_id = project.id
  and chatbot.workspace_id is null;

alter table public.chatbot_config
    alter column name set not null,
    alter column access_code_hash set not null,
    alter column workspace_id set not null;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conrelid = 'public.chatbot_config'::regclass
          and conname = 'chatbot_config_access_code_hash_key'
    ) then
        alter table public.chatbot_config
            add constraint chatbot_config_access_code_hash_key unique (access_code_hash);
    end if;
    if not exists (
        select 1 from pg_constraint
        where conrelid = 'public.chatbot_config'::regclass
          and conname = 'chatbot_config_access_code_hash_length_check'
    ) then
        alter table public.chatbot_config
            add constraint chatbot_config_access_code_hash_length_check
            check (char_length(access_code_hash) = 64);
    end if;
end $$;

create index if not exists idx_chatbot_config_workspace_id
    on public.chatbot_config(workspace_id);
create index if not exists idx_chatbot_config_workspace_active
    on public.chatbot_config(workspace_id, is_active);

create table public.chatbot_enrollments (
    id uuid primary key default gen_random_uuid(),
    chatbot_id uuid not null references public.chatbot_config(id) on delete cascade,
    user_id uuid not null references public.app_users(id) on delete cascade,
    joined_at timestamp not null default current_timestamp,
    constraint chatbot_enrollments_chatbot_user_key unique (chatbot_id, user_id)
);

-- A unique acima atende buscas por chatbot; este índice atende a listagem do usuário.
create index idx_chatbot_enrollments_user_id
    on public.chatbot_enrollments(user_id);

alter table public.chatbot_enrollments enable row level security;
revoke all on table public.chatbot_enrollments from anon, authenticated;

alter table public.chat_messages
    add column if not exists chatbot_id uuid
        references public.chatbot_config(id) on delete set null;

create index if not exists idx_chat_messages_chatbot_asked_at
    on public.chat_messages(chatbot_id, asked_at desc);
create index if not exists idx_chat_messages_chatbot_user_asked_at
    on public.chat_messages(chatbot_id, user_email, asked_at desc);

commit;
