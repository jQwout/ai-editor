CREATE TABLE IF NOT EXISTS prompt_store_prompts (
  id uuid PRIMARY KEY,
  mode_id text NOT NULL,
  model_id text NULL,
  prompt_text text NOT NULL,
  temperature double precision NOT NULL,
  is_enabled boolean NOT NULL DEFAULT true,
  tags jsonb NOT NULL DEFAULT '[]'::jsonb,
  origin jsonb NOT NULL DEFAULT '{}'::jsonb,
  meta jsonb NOT NULL DEFAULT '{}'::jsonb,
  raw jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- Enforce single base prompt per mode (model_id is null)
CREATE UNIQUE INDEX IF NOT EXISTS ux_prompt_store_base_mode
  ON prompt_store_prompts (mode_id)
  WHERE model_id IS NULL;

-- Enforce single override per (mode, model) (model_id is not null)
CREATE UNIQUE INDEX IF NOT EXISTS ux_prompt_store_override_mode_model
  ON prompt_store_prompts (mode_id, model_id)
  WHERE model_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_prompt_store_mode_id ON prompt_store_prompts (mode_id);
CREATE INDEX IF NOT EXISTS ix_prompt_store_model_id ON prompt_store_prompts (model_id);
