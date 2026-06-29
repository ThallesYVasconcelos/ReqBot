-- Busca literal por projeto e busca vetorial com índice de baixo consumo.

create index if not exists idx_requirements_full_text
    on public.requirements using gin (
        to_tsvector(
            'simple',
            coalesce(requirement_id, '') || ' ' ||
            coalesce(refined_requirement, '') || ' ' ||
            coalesce(raw_requirement, '')
        )
    );

create index if not exists idx_embeddings_project_id
    on public.embeddings ((metadata ->> 'project_id'));

create index if not exists idx_embeddings_requirement_uuid
    on public.embeddings ((metadata ->> 'requirement_uuid'));

-- M e ef_construction menores reduzem memória de construção no plano gratuito.
create index if not exists idx_embeddings_hnsw_cosine
    on public.embeddings using hnsw (embedding vector_cosine_ops)
    with (m = 8, ef_construction = 32)
    where embedding is not null;
