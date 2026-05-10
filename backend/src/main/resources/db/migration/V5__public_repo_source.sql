create table if not exists public_repo_source (
  id bigserial primary key,
  repo_url text not null unique,
  provider text not null default 'github',
  default_branch text,
  last_seen_commit text,
  last_ingested_commit text,
  last_checked_at timestamptz,
  last_ingested_at timestamptz,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists public_repo_source_enabled_idx
  on public_repo_source(is_enabled) where is_enabled = true;

create index if not exists public_repo_source_updated_at_idx
  on public_repo_source(updated_at);
