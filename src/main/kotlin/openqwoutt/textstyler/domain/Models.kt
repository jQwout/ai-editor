package openqwoutt.miniapp.textstyler.domain

enum class ModeGroup {
    /** Main modes: style, fix */
    MAIN,
    /** Base rewrite + tone presets */
    STYLE,
    /** Grammar / clarity fix */
    FIX,
    /** Summary + deep analyze */
    ANALYSIS
}

enum class StyleMode(
    val id: String,
    val displayName: String,
    val shortName: String,
    val icon: String,
    val group: ModeGroup,
    val prompt: String,
    val temperature: Double = 0.4
) {
    STYLE(
        id = "style",
        displayName = "Clean style",
        shortName = "Style",
        icon = "*",
        group = ModeGroup.MAIN,
        prompt = "Rewrite the text to sound polished, clear, and modern. Preserve the original meaning. Respond in the same language as the input text. Return only the rewritten text."
    ),
    FIX(
        id = "fix",
        displayName = "Fix grammar",
        shortName = "Fix",
        icon = "o",
        group = ModeGroup.MAIN,
        prompt = "Fix all spelling, grammar, punctuation, and clarity errors. Preserve the original meaning. Respond in the same language as the input text. Return only the corrected text without explanations."
    ),
    FORMAL(
        id = "style_formal",
        displayName = "Formal",
        shortName = "Formal",
        icon = "F",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in a formal, professional business style. Be polite and respectful. Respond in the same language as the input text. Return only the result."
    ),
    SHORT(
        id = "style_short",
        displayName = "Short",
        shortName = "Short",
        icon = "S",
        group = ModeGroup.STYLE,
        prompt = "Make the text shorter, sharper, and easier to scan. Preserve the key meaning and all important information. Respond in the same language as the input text. Return only the shortened result."
    ),
    TRIBAL(
        id = "style_tribal",
        displayName = "Tribal",
        shortName = "Tribal",
        icon = "T",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text with vivid, primal, clan-like energy. Make it sound passionate and collective. Respond in the same language as the input text. Return only the result.",
        temperature = 0.7
    ),
    CORP(
        id = "style_corp",
        displayName = "Corp",
        shortName = "Corp",
        icon = "C",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in concise corporate language suitable for work messages. Use clear, direct phrasing. Respond in the same language as the input text. Return only the result."
    ),
    BIBLICAL(
        id = "style_biblical",
        displayName = "Biblical",
        shortName = "Biblical",
        icon = "B",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in an elevated biblical cadence. Use flowing, timeless phrasing without adding religious claims. Respond in the same language as the input text. Return only the result.",
        temperature = 0.7
    ),
    VIKING(
        id = "style_viking",
        displayName = "Viking",
        shortName = "Viking",
        icon = "V",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text with bold old-norse saga energy. Use strong, heroic phrasing. Respond in the same language as the input text. Return only the result.",
        temperature = 0.7
    ),
    ZEN(
        id = "style_zen",
        displayName = "Zen",
        shortName = "Zen",
        icon = "Z",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in a calm, minimal, grounded tone. Use sparse, peaceful language. Respond in the same language as the input text. Return only the result."
    ),
    OLD_EMOJI(
        id = "style_old_emoji",
        displayName = "Emojify",
        shortName = "Emojify",
        icon = ":)",
        group = ModeGroup.STYLE,
        prompt = "Add fitting old-school emoticons like :-) :-/ :-D T_T ^_^ to convey emotion. Do NOT change the words or add new text. Respond in the same language as the input text. Return only the modified text.",
        temperature = 0.6
    ),
    SUMMARIZE(
        id = "summarize",
        displayName = "Summarize",
        shortName = "Summary",
        icon = "=",
        group = ModeGroup.ANALYSIS,
        prompt = "Create a clear, concise summary of the text. Capture all key information. Use bullets if that helps clarity. Respond in the same language as the input text. Return only the summary."
    ),
    ANALYZE(
        id = "analyze",
        displayName = "Analyze",
        shortName = "Analyze",
        icon = "?",
        group = ModeGroup.ANALYSIS,
        prompt = "Analyze the text for: main intent and purpose, tone and emotional register, key points (3-5 bullets max), weak spots and potential issues, suggested improvements. Respond in the same language as the input text. Keep the response concise and actionable."
    ),

}

sealed class TextStylerResult {
    data class Success(val result: String) : TextStylerResult()
    data class Failure(val message: String) : TextStylerResult()
    data object EmptyInput : TextStylerResult()
    data object OrchestratorFailed : TextStylerResult()
}
