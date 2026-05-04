package openqwoutt.miniapp.textstyler.domain

enum class ModeGroup {
    MAIN,
    STYLE,
    ANALYZE
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
    ANALYZE_MAIN(
        id = "analyze",
        displayName = "Analyze",
        shortName = "Analyze",
        icon = "?",
        group = ModeGroup.MAIN,
        prompt = "Analyze the text: intent, tone, key points, weak spots, and suggested improvements. Keep it concise."
    ),
    STYLE(
        id = "style",
        displayName = "Clean style",
        shortName = "Style",
        icon = "*",
        group = ModeGroup.MAIN,
        prompt = "Rewrite the text to sound polished, clear, and modern while preserving the meaning. Return only the rewritten text."
    ),
    FIX(
        id = "fix",
        displayName = "Fix grammar",
        shortName = "Fix",
        icon = "o",
        group = ModeGroup.MAIN,
        prompt = "Fix spelling, grammar, punctuation, and clarity. Return only the corrected text without explanations."
    ),
    FORMAL(
        id = "style_formal",
        displayName = "Formal",
        shortName = "Formal",
        icon = "F",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in a formal business style. Return only the result."
    ),
    SHORT(
        id = "style_short",
        displayName = "Short",
        shortName = "Short",
        icon = "S",
        group = ModeGroup.STYLE,
        prompt = "Make the text shorter, sharper, and easier to scan. Preserve the key meaning. Return only the result."
    ),
    TRIBAL(
        id = "style_tribal",
        displayName = "Tribal",
        shortName = "Tribal",
        icon = "T",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text with vivid, primal, clan-like energy while keeping it readable and respectful. Return only the result.",
        temperature = 0.7
    ),
    CORP(
        id = "style_corp",
        displayName = "Corp",
        shortName = "Corp",
        icon = "C",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in concise corporate language suitable for work messages. Return only the result."
    ),
    BIBLICAL(
        id = "style_biblical",
        displayName = "Biblical",
        shortName = "Biblical",
        icon = "B",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in an elevated biblical cadence without adding religious claims. Return only the result.",
        temperature = 0.7
    ),
    VIKING(
        id = "style_viking",
        displayName = "Viking",
        shortName = "Viking",
        icon = "V",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text with bold old-norse saga energy while preserving the meaning. Return only the result.",
        temperature = 0.7
    ),
    ZEN(
        id = "style_zen",
        displayName = "Zen",
        shortName = "Zen",
        icon = "Z",
        group = ModeGroup.STYLE,
        prompt = "Rewrite the text in a calm, minimal, grounded tone. Return only the result."
    ),
    OLD_EMOJI(
        id = "style_old_emoji",
        displayName = "Emojify",
        shortName = "Emojify",
        icon = ":)",
        group = ModeGroup.STYLE,
        prompt = "Add fitting old-school emoticons such as T_T, ^_^, :-) without changing the wording. Return only the modified text.",
        temperature = 0.6
    ),
    SUMMARIZE(
        id = "summarize",
        displayName = "Summarize",
        shortName = "Summary",
        icon = "=",
        group = ModeGroup.ANALYZE,
        prompt = "Summarize the text into the clearest useful version. Use short bullets only if that helps. Return only the summary."
    )
}

sealed class TextStylerResult {
    data class Success(val result: String) : TextStylerResult()
    data object EmptyInput : TextStylerResult()
    data object OrchestratorFailed : TextStylerResult()
}
