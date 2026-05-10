package openqwoutt.textprocessor.backend.textprocessing.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProcessTextErrorDiagnosticsTest {

    @Test
    fun `truncate leaves short string unchanged`() {
        val s = "hello"
        assertEquals(s, ProcessTextErrorDiagnostics.truncateProviderRaw(s))
    }

    @Test
    fun `truncate appends marker when over limit`() {
        val raw = "a".repeat(ProcessTextErrorDiagnostics.MAX_PROVIDER_RAW_CHARS + 50)
        val out = ProcessTextErrorDiagnostics.truncateProviderRaw(raw)!!
        assertTrue(out.endsWith("chars]"))
        assertTrue(out.contains("truncated 50"))
        assertTrue(out.length > ProcessTextErrorDiagnostics.MAX_PROVIDER_RAW_CHARS)
    }

    @Test
    fun `truncate null stays null`() {
        assertEquals(null, ProcessTextErrorDiagnostics.truncateProviderRaw(null))
    }
}
