package cl.controlacceso.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrParserTest {

    @Test
    fun parsesPlainPmiId() {
        assertEquals("1234567", QrParser.parsePmiId("1234567"))
    }

    @Test
    fun parsesPrefixedPayload() {
        assertEquals("1234567", QrParser.parsePmiId("PMI:1234567"))
    }

    @Test
    fun rejectsInvalidPayloads() {
        assertNull(QrParser.parsePmiId("https://registrocivil.cl/?RUN=1-9"))
        assertNull(QrParser.parsePmiId("abc"))
        assertNull(QrParser.parsePmiId(""))
        assertNull(QrParser.parsePmiId(null))
    }

    @Test
    fun buildPayloadReturnsDigitsOnly() {
        assertEquals("1234567", QrParser.buildPayload("1234567"))
    }
}
