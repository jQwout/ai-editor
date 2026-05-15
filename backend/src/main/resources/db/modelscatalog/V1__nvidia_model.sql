create table if not exists nvidia_model (
  id bigserial primary key,
  model_id text not null unique,
  display_name text not null,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists nvidia_model_enabled_idx
  on nvidia_model (is_enabled)
  where is_enabled = true;
