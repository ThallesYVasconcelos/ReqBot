-- Project enrollments and single-owner-workspace invariant.
-- Users enter a project by using any chatbot code that belongs to that project.

begin;

create table if not exists public.project_enrollments (
    id uuid primary key default gen_random_uuid(),
    requirement_set_id uuid not null references public.requirement_sets(id) on delete cascade,
    user_id uuid not null references public.app_users(id) on delete cascade,
    joined_via_chatbot_id uuid references public.chatbot_config(id) on delete set null,
    joined_at timestamp not null default current_timestamp,
    constraint project_enrollments_project_user_key unique (requirement_set_id, user_id)
);

create index if not exists idx_project_enrollments_requirement_set_id
    on public.project_enrollments(requirement_set_id);
create index if not exists idx_project_enrollments_user_id
    on public.project_enrollments(user_id);
create index if not exists idx_project_enrollments_joined_via_chatbot_id
    on public.project_enrollments(joined_via_chatbot_id);

insert into public.project_enrollments (
    requirement_set_id,
    user_id,
    joined_via_chatbot_id,
    joined_at
)
select
    chatbot.requirement_set_id,
    enrollment.user_id,
    enrollment.chatbot_id,
    min(enrollment.joined_at)
from public.chatbot_enrollments enrollment
join public.chatbot_config chatbot
  on chatbot.id = enrollment.chatbot_id
where chatbot.requirement_set_id is not null
group by chatbot.requirement_set_id, enrollment.user_id, enrollment.chatbot_id
on conflict (requirement_set_id, user_id) do nothing;

alter table public.project_enrollments enable row level security;
revoke all on table public.project_enrollments from anon, authenticated;

create unique index if not exists ux_workspace_members_single_owner_per_user
    on public.workspace_members(user_id)
    where role = 'OWNER';

commit;
