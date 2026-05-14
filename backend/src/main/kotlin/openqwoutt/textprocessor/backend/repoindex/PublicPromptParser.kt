package openqwoutt.textprocessor.backend.repoindex

/**
 * Универсальный “грязный” парсер промтов для публичных реп:
 * - CSV: заголовки `id|name|title`, `prompt|prompt_text|text`, `tags`
 * - Markdown: секции по headings (`#`..`######`) + первый code block как тело промта (иначе абзацы)
 *
 * Возвращает entries с заполненными `path/sha/source` снаружи (индексатором).
 */
object PublicPromptParser {

    data class ParsedPrompt(
        val title: String?,
        val promptText: String,
        val tags: List<String>,
    )

    fun parseByPath(path: String, content: String): List<ParsedPrompt> {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".csv") -> parseCsv(content)
            lower.endsWith(".md") || lower.endsWith(".markdown") -> parseMarkdown(content)
            else -> emptyList()
        }
    }

    fun parseCsv(content: String): List<ParsedPrompt> {
        val lines = content.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return emptyList()

        val header = splitCsvLine(lines.first())
        val idxPrompt = header.indexOfFirst { it.equals("prompt", true) || it.equals("prompt_text", true) || it.equals("text", true) }
        if (idxPrompt < 0) return emptyList()
        val idxTitle = header.indexOfFirst { it.equals("title", true) || it.equals("name", true) || it.equals("id", true) }
        val idxTags = header.indexOfFirst { it.equals("tags", true) || it.equals("tag", true) }

        return lines.drop(1).mapNotNull { line ->
            val cols = splitCsvLine(line)
            val prompt = cols.getOrNull(idxPrompt)?.trim().orEmpty()
            if (prompt.isBlank()) return@mapNotNull null
            val title = cols.getOrNull(idxTitle)?.trim()?.takeIf { it.isNotBlank() }
            val tags = cols.getOrNull(idxTags)
                ?.split(',', ';')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                .orEmpty()
            ParsedPrompt(title = title, promptText = prompt, tags = tags)
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        // Минимальная поддержка CSV: запятые + кавычки (без вложенных переносов строк).
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    out.add(sb.toString()); sb.setLength(0)
                }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    fun parseMarkdown(content: String): List<ParsedPrompt> {
        val text = content.replace("\r\n", "\n")
        val blocks = splitMarkdownSections(text)
        return blocks.mapNotNull { section ->
            val title = section.heading?.trim()?.takeIf { it.isNotBlank() }
            val (code, rest) = extractFirstFencedCodeBlock(section.body)
            val promptText = (code ?: rest).trim()
            if (promptText.isBlank()) return@mapNotNull null
            val tags = extractInlineTags(section.body)
            ParsedPrompt(title = title, promptText = promptText, tags = tags)
        }
    }

    private data class MdSection(val heading: String?, val body: String)

    private fun splitMarkdownSections(text: String): List<MdSection> {
        val lines = text.split('\n')
        val sections = ArrayList<MdSection>()
        var curHeading: String? = null
        val cur = StringBuilder()

        fun flush() {
            val body = cur.toString().trim()
            if (body.isNotBlank()) sections.add(MdSection(curHeading, body))
            cur.setLength(0)
        }

        for (line in lines) {
            val m = Regex("""^(#{1,6})\s+(.*)$""").matchEntire(line.trimEnd())
            if (m != null) {
                flush()
                curHeading = m.groupValues[2]
            } else {
                cur.appendLine(line)
            }
        }
        flush()
        if (sections.isEmpty() && text.isNotBlank()) {
            return listOf(MdSection(null, text.trim()))
        }
        return sections
    }

    private fun extractFirstFencedCodeBlock(body: String): Pair<String?, String> {
        val lines = body.split('\n')
        val rest = StringBuilder()
        val code = StringBuilder()
        var inFence = false
        var fenceFound = false

        for (line in lines) {
            val trimmed = line.trim()
            if (!fenceFound && trimmed.startsWith("```")) {
                inFence = true
                fenceFound = true
                continue
            }
            if (inFence && trimmed.startsWith("```")) {
                inFence = false
                continue
            }
            if (inFence) code.appendLine(line) else rest.appendLine(line)
        }
        val codeText = code.toString().trim().takeIf { it.isNotBlank() }
        return codeText to rest.toString()
    }

    private fun extractInlineTags(body: String): List<String> {
        // #tag внутри текста/markdown (кроме headings)
        val tags = Regex("""(?<!\w)#([a-zA-Z0-9_\-]+)""")
            .findAll(body)
            .map { it.groupValues[1] }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        return tags
    }
}

