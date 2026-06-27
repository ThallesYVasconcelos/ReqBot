-- Day 1: identidade global simples e autorização OWNER/ADMIN por workspace.

begin;

-- O papel global deixa de conceder privilégios de tenant.
update public.app_users
set role = 'USER'
where role <> 'USER';

-- Preserva workspaces e associações legadas antes de remover e-mails duplicados.
insert into public.app_users (email, name, role, created_at)
select distinct lower(trim(source.email)), null, 'USER', current_timestamp
from (
    select owner_email as email from public.workspaces
    union
    select user_email as email from public.workspace_members
) source
where source.email is not null
  and trim(source.email) <> ''
on conflict (email) do nothing;

alter table public.workspace_members
    add column if not exists user_id uuid;

update public.workspace_members member
set user_id = app_user.id
from public.app_users app_user
where member.user_id is null
  and lower(trim(member.user_email)) = lower(trim(app_user.email));

-- Garante exatamente a associação OWNER representada anteriormente em workspaces.
insert into public.workspace_members (workspace_id, user_email, user_id, role, created_at)
select workspace.id, lower(trim(workspace.owner_email)), app_user.id, 'OWNER', current_timestamp
from public.workspaces workspace
join public.app_users app_user
  on lower(trim(app_user.email)) = lower(trim(workspace.owner_email))
where not exists (
    select 1
    from public.workspace_members member
    where member.workspace_id = workspace.id
      and member.user_id = app_user.id
)
on conflict do nothing;

update public.workspace_members member
set role = 'OWNER'
from public.workspaces workspace, public.app_users app_user
where member.workspace_id = workspace.id
  and member.user_id = app_user.id
  and lower(trim(app_user.email)) = lower(trim(workspace.owner_email));

-- Usuários finais não pertencem ao workspace; o acesso ao chatbot será próprio.
delete from public.workspace_members where role = 'MEMBER';

alter table public.workspace_members
    alter column user_id set not null;

alter table public.workspace_members
    drop constraint if exists workspace_members_workspace_id_user_email_key;

do $$
declare
    constraint_name text;
begin
    for constraint_name in
        select conname
        from pg_constraint
        where conrelid = 'public.workspace_members'::regclass
          and contype = 'c'
          and pg_get_constraintdef(oid) ilike '%role%'
    loop
        execute format('alter table public.workspace_members drop constraint %I', constraint_name);
    end loop;
end $$;

alter table public.workspace_members
    add constraint workspace_members_user_id_fkey
        foreign key (user_id) references public.app_users(id) on delete cascade,
    add constraint workspace_members_workspace_user_key
        unique (workspace_id, user_id),
    add constraint workspace_members_role_check
        check (role in ('OWNER', 'ADMIN'));

create index if not exists idx_workspace_members_user_id
    on public.workspace_members(user_id);

create unique index if not exists uq_workspace_members_one_owner
    on public.workspace_members(workspace_id)
    where role = 'OWNER';

drop index if exists public.idx_workspace_members_user_email;

alter table public.workspace_members
    drop column if exists user_email;

drop index if exists public.idx_workspaces_owner_email;

alter table public.workspaces
    drop column if exists owner_email,
    drop column if exists invite_code;

commit;
