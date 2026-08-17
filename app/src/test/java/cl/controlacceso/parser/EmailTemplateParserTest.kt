package cl.controlacceso.parser

import cl.controlacceso.parser.EmailTemplateParseResult.Failure
import cl.controlacceso.parser.EmailTemplateParseResult.Success
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EmailTemplateParserTest {

    @Test
    fun firstLineIsSubjectRemainingIsBody() {
        val raw = "Asunto de prueba\nLínea 1\n\nLínea 3\n"
        val result = EmailTemplateParser.parse(raw)
        assertTrue(result is Success)
        val template = (result as Success).template
        assertEquals("Asunto de prueba", template.subject)
        assertEquals("Línea 1\n\nLínea 3\n", template.body)
        assertTrue(template.fromFile)
    }

    @Test
    fun preservesWindowsCrlfInBody() {
        val raw = "Asunto\r\nEstimado/a ****NOMBRE****:\r\n\r\nSaludos\r\n"
        val result = EmailTemplateParser.parse(raw)
        assertTrue(result is Success)
        val template = (result as Success).template
        assertEquals("Asunto", template.subject)
        assertEquals("Estimado/a ****NOMBRE****:\r\n\r\nSaludos\r\n", template.body)
    }

    @Test
    fun personalizesNameAndKeepsLineBreaks() {
        val body = "Estimado/a ****NOMBRE****:\nTexto"
        val personalized = EmailTemplateParser.personalize(body, "Maria")
        assertEquals("Estimado/a Maria:\nTexto", personalized)
    }

    @Test
    fun appendsBoldQrNoticeWithoutRemovingBody() {
        val body = "Cuerpo original\nSegunda línea"
        val withNotice = EmailTemplateParser.appendQrNotice(body)
        assertTrue(withNotice.startsWith(body))
        assertTrue(withNotice.contains(EmailTemplateParser.QR_ATTACHMENT_NOTICE))
        val html = EmailTemplateParser.toHtml(withNotice)
        assertTrue(html.contains("<strong>${EmailTemplateParser.QR_ATTACHMENT_NOTICE}</strong>"))
        assertTrue(html.contains("Cuerpo original<br/>"))
    }

    @Test
    fun usesDefaultWhenNoFile() {
        val template = EmailTemplateParser.defaultTemplate()
        assertFalse(template.fromFile)
        assertEquals(EmailTemplateParser.DEFAULT_SUBJECT, template.subject)
        assertTrue(template.body.contains("Estimado/a"))
        assertTrue(template.body.contains("actividad del PMI"))
    }

    @Test
    fun rejectsEmptyFile() {
        val result = EmailTemplateParser.parse("   \n  ")
        assertTrue(result is Failure)
    }

    @Test
    fun exampleEmailFileIsImportable() {
        val candidates = listOf(
            File("examples/email.example.txt"),
            File("../examples/email.example.txt"),
            File("../../examples/email.example.txt")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("No se encontró examples/email.example.txt")
        val result = EmailTemplateParser.parse(file.readText())
        assertTrue(result is Success)
        val template = (result as Success).template
        assertEquals(
            "Invitación – Encuentro de Voluntarios PMI | 5 de septiembre de 2026",
            template.subject
        )
        assertFalse(template.body.contains(template.subject))
        assertTrue(template.body.contains("****NOMBRE****"))
        assertTrue(template.body.contains("Saludos cordiales"))
        val personalized = EmailTemplateParser.personalize(template.body, "Maria")
        assertTrue(personalized.contains("Estimado/a Maria"))
        assertFalse(personalized.contains("****NOMBRE****"))
    }
}
