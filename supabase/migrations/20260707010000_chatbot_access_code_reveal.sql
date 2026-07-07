-- Guarda o codigo original do chatbot para permitir que admins/owners o vejam novamente.
-- Registros antigos continuam com NULL porque hashes anteriores nao sao reversiveis.

begin;

alter table public.chatbot_config
    add column if not exists access_code varchar(11);

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conrelid = 'public.chatbot_config'::regclass
          and conname = 'chatbot_config_access_code_format_check'
    ) then
        alter table public.chatbot_config
            add constraint chatbot_config_access_code_format_check
            check (access_code is null or access_code ~ '^[A-Z2-9]{5}-[A-Z2-9]{5}$');
    end if;
end $$;

commit;
