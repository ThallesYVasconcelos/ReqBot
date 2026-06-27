-- Convites de ADMIN são limitados ao workspace e armazenam somente o hash do token.

create table public.workspace_admin_invitations (
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null references public.workspaces(id) on delete cascade,
    invited_email varchar(255) not null,
    token_hash varchar(64) not null unique,
    status varchar(20) not null default 'PENDING'
        check (status in ('PENDING', 'ACCEPTED', 'EXPIRED')),
    expires_at timestamp not null,
    invited_by_user_id uuid not null references public.app_users(id) on delete cascade,
    accepted_by_user_id uuid references public.app_users(id) on delete set null,
    created_at timestamp not null default current_timestamp,
    accepted_at timestamp
);

create index idx_workspace_admin_invitations_workspace_status
    on public.workspace_admin_invitations(workspace_id, status);
create index idx_workspace_admin_invitations_email_status
    on public.workspace_admin_invitations(invited_email, status);
create index idx_workspace_admin_invitations_invited_by
    on public.workspace_admin_invitations(invited_by_user_id);
create index idx_workspace_admin_invitations_accepted_by
    on public.workspace_admin_invitations(accepted_by_user_id)
    where accepted_by_user_id is not null;

alter table public.workspace_admin_invitations enable row level security;
