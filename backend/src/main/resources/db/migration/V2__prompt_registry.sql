-- Replace repo file index with prompt registry (models + prompts + per-model overrides)

drop table if exists repo_file cascade;
drop table if exists repo_source cascade;

create table if not exists ai_model (
  id bigserial primary key,
  model_id text not null unique,
  display_name text not null,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists ai_model_enabled_idx on ai_model(is_enabled) where is_enabled = true;

create table if not exists prompt (
  id bigserial primary key,
  mode_id text not null unique,
  prompt_text text not null,
  temperature double precision not null default 0.4,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists model_prompt (
  model_id bigint not null references ai_model(id) on delete cascade,
  mode_id text not null,
  prompt_text_override text,
  temperature_override double precision,
  primary key (model_id, mode_id)
);

-- Seed base prompts (aligned with backend StyleMode / Android StyleMode ids)
insert into prompt (mode_id, prompt_text, temperature) values
('style', 'Rewrite the text to sound polished, clear, and modern. Preserve the original meaning. Respond in the same language as the input text. Return only the rewritten text.', 0.4),
('fix', 'Fix all spelling, grammar, punctuation, and clarity errors. Preserve the original meaning. Respond in the same language as the input text. Return only the corrected text without explanations.', 0.4),
('style_formal', 'Rewrite the text in a formal, professional business style. Be polite and respectful. Respond in the same language as the input text. Return only the result.', 0.4),
('style_short', 'Make the text shorter, sharper, and easier to scan. Preserve the key meaning and all important information. Respond in the same language as the input text. Return only the shortened result.', 0.4),
('style_tribal', 'Rewrite the text with vivid, primal, clan-like energy. Make it sound passionate and collective. Respond in the same language as the input text. Return only the result.', 0.7),
('style_corp', 'Rewrite the text in concise corporate language suitable for work messages. Use clear, direct phrasing. Respond in the same language as the input text. Return only the result.', 0.4),
('style_biblical', 'Rewrite the text in an elevated biblical cadence. Use flowing, timeless phrasing without adding religious claims. Respond in the same language as the input text. Return only the result.', 0.7),
('style_viking', 'Rewrite the text with bold old-norse saga energy. Use strong, heroic phrasing. Respond in the same language as the input text. Return only the result.', 0.7),
('style_zen', 'Rewrite the text in a calm, minimal, grounded tone. Use sparse, peaceful language. Respond in the same language as the input text. Return only the result.', 0.4),
('style_old_emoji', 'Add fitting old-school emoticons like :-) :-/ :-D T_T ^_^ to convey emotion. Do NOT change the words or add new text. Respond in the same language as the input text. Return only the modified text.', 0.6),
('summarize', 'Create a clear, concise summary of the text. Capture all key information. Use bullets if that helps clarity. Respond in the same language as the input text. Return only the summary.', 0.4),
('analyze', 'Analyze the text for: main intent and purpose, tone and emotional register, key points (3-5 bullets max), weak spots and potential issues, suggested improvements. Respond in the same language as the input text. Keep the response concise and actionable.', 0.4),
('screenshot_analysis', 'Act as a screen-aware assistant. The user may paste OCR text or a description from a screenshot. Explain what is visible, what matters, and what action to take next. IMPORTANT: Always respond in the SAME language as the user''s input text.', 0.4)
on conflict (mode_id) do nothing;

-- Seed example models (admin can change via API)
insert into ai_model (model_id, display_name, is_enabled) values
('google/gemini-2.0-flash-exp:free', 'Gemini 2.0 Flash (free)', true),
('deepseek/deepseek-chat:free', 'DeepSeek Chat (free)', true),
('meta-llama/llama-3.1-8b-instruct:free', 'Llama 3.1 8B Instruct (free)', true)
on conflict (model_id) do nothing;
