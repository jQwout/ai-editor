-- Allow multiple prompts per file/blob (CSV rows / multiple markdown sections)

alter table public_prompt
  add column if not exists item_key text not null default '';

do $$
begin
  -- default name for "unique(source, path, sha)" in Postgres:
  if exists (
    select 1
    from pg_constraint
    where conname = 'public_prompt_source_path_sha_key'
  ) then
    alter table public_prompt drop constraint public_prompt_source_path_sha_key;
  end if;
exception when undefined_table then
  -- table might not exist in some environments
  null;
end $$;

create unique index if not exists public_prompt_uni_idx
  on public_prompt(source, path, sha, item_key);

create index if not exists public_prompt_item_key_idx on public_prompt(item_key);
