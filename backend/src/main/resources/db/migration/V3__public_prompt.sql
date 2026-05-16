create table if not exists public_prompt (
  id bigserial primary key,
  source text not null,
  path text not null,
  sha text not null,
  tags text[] not null default '{}',
  prompt_text text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(source, path, sha)
);

create index if not exists public_prompt_source_idx on public_prompt(source);
create index if not exists public_prompt_path_idx on public_prompt(path);
create index if not exists public_prompt_updated_at_idx on public_prompt(updated_at);
