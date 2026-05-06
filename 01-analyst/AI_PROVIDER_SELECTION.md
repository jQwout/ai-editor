# AI Provider Selection Feature Specification

## Goal
Add ability to select AI provider (OpenRouter, Pollinations, UncloseAI, G4F, Groq) from settings.

## Requirements

### 1. AiProvider Enum
Add enum with all supported providers:
- **OpenRouter** (existing) - requires API key
- **Pollinations** - no registration, free
- **UncloseAI** - no registration, free  
- **G4F** - no registration (uses HF Space)
- **Groq** - requires API key, 30 req/min

Each provider stores:
- `displayName` - user-friendly name
- `baseUrl` - API endpoint
- `model` - default model
- `requiresApiKey` - whether key is needed

### 2. AppSettings
Add fields:
- `aiProvider: AiProvider = AiProvider.OpenRouter`
- `apiKey: String = ""` - for providers requiring key

### 3. TextProcessorUseCase
Modify to handle different API formats:
- OpenRouter: uses `/v1/chat/completions` with custom format
- Others: OpenAI-compatible `/v1/chat/completions`

### 4. Settings UI
Add dropdown/selector for AI provider in settings screen.

## Acceptance Criteria
1. User can select AI provider from dropdown
2. If provider requires API key - show API key input field
3. Selected provider is saved in settings
4. Text processing uses selected provider's API