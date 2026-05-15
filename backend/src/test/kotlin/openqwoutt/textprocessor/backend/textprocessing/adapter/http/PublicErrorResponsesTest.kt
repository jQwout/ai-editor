package openqwoutt.textprocessor.backend.textprocessing.adapter.http

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextErrorKind
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PublicErrorResponsesTest {

    private fun err(
        kind: ProcessTextErrorKind,
        message: String,
        providerId: String? = null,
        httpStatus: Int? = null,
        providerRaw: String? = """{"x":1}""",
        detail: String? = null,
    ) = ProcessTextOutcome.Err(
        kind = kind,
        message = message,
        cause = null,
        providerId = providerId,
        httpStatus = httpStatus,
        providerRaw = providerRaw,
        detail = detail,
    )

    @Test
    fun `non ai errors map to dto`() {
        val dto =
            buildProcessTextErrorResponse(
                err(ProcessTextErrorKind.BAD_REQUEST, "bad"),
            )
        assertEquals("bad", dto.error)
        assertNull(dto.providerId)
    }

    @Test
    fun `ai failure passes through provider payload`() {
        val dto =
            buildProcessTextErrorResponse(
                err(ProcessTextErrorKind.AI_BACKEND_FAILED, "fail", providerId = "openrouter", httpStatus = 401),
            )
        assertEquals("fail", dto.error)
        assertEquals("openrouter", dto.providerId)
        assertEquals(401, dto.httpStatus)
        assertEquals("""{"x":1}""", dto.providerRaw)
        assertNotNull(dto.providerBody)
        assertNull(dto.detail)
    }

    @Test
    fun `ai failure passes through nvidia error payload`() {
        val full =
            buildProcessTextErrorResponse(
                err(ProcessTextErrorKind.AI_BACKEND_FAILED, "fail", providerId = "nvidia", httpStatus = 500),
            )
        assertNotNull(full.providerRaw)
        assertNotNull(full.providerBody)
    }

    @Test
    fun `ai failure parses provider json body`() {
        val full =
            buildProcessTextErrorResponse(
                err(ProcessTextErrorKind.AI_BACKEND_FAILED, "e", providerId = "p", httpStatus = 502),
            )
        assertEquals(1, full.providerBody!!.jsonObject["x"]!!.jsonPrimitive.int)
    }
}
