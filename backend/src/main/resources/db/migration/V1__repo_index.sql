create table if not exists repo_source (
  id bigserial primary key,
  repo_url text not null unique,
  commits_atom_url text not null,
  releases_atom_url text,
  default_branch text not null default 'main',
  local_path text not null,
  last_seen_commit text,
  last_ingested_commit text,
  last_checked_at timestamptz,
  last_ingested_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists repo_source_updated_at_idx on repo_source(updated_at);

create table if not exists repo_file (
  id bigserial primary key,
  repo_id bigint not null references repo_source(id) on delete cascade,
  path text not null,
  ext text not null,
  size_bytes bigint not null,
  blob_sha text,
  content_hash text not null,
  content_text text,
  is_binary boolean not null default false,
  is_deleted boolean not null default false,
  updated_at timestamptz not null default now(),
  unique(repo_id, path)
);

create index if not exists repo_file_repo_path_idx on repo_file(repo_id, path);
create index if not exists repo_file_repo_ext_idx on repo_file(repo_id, ext);
