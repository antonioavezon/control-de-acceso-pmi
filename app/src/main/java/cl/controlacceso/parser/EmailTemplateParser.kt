package cl.controlacceso.parser

import cl.controlacceso.model.EmailTemplate

sealed class EmailTemplateParseResult {
    data class Success(val template: EmailTemplate) : EmailTemplateParseResult()
    data class Failure(val message: String) : EmailTemplateParseResult()
}

object EmailTemplateParser {

    const val QR_ATTACHMENT_NOTICE =
        "El código QR personal va adjunto en este correo."

    const val DEFAULT_SUBJECT = "Invitación PMI"

    val DEFAULT_BODY = listOf(
        "Estimado/a",
        "Se le invita a una actividad del PMI, para lo cual deberá presentar el QR enviado adjunto, debrá presentarlo en su celular.",
        "Saludos cordiales"
    ).joinToString("\n")

    fun defaultTemplate(): EmailTemplate = EmailTemplate(
        subject = DEFAULT_SUBJECT,
        body = DEFAULT_BODY,
        fromFile = false
    )

    fun parse(content: String?): EmailTemplateParseResult {
        if (content == null) {
            return EmailTemplateParseResult.Failure("No fue posible leer el archivo de correo.")
        }
        if (content.isBlank()) {
            return EmailTemplateParseResult.Failure("El archivo de correo está vacío.")
        }

        val breakInfo = firstLineBreak(content)
        val subject: String
        val body: String
        if (breakInfo == null) {
            subject = content.trim()
            body = ""
        } else {
            subject = content.substring(0, breakInfo.lineEnd).trim()
            body = content.substring(breakInfo.nextContent)
        }

        if (subject.isEmpty()) {
            return EmailTemplateParseResult.Failure(
                "La primera línea del archivo debe contener el asunto del correo."
            )
        }

        return EmailTemplateParseResult.Success(
            EmailTemplate(subject = subject, body = body, fromFile = true)
        )
    }

    fun personalize(body: String, nombre: String): String {
        val safeName = nombre.trim()
        return body
            .replace("****NOMBRE****", safeName)
            .replace("__NOMBRE__", safeName)
            .replace("[NOMBRE]", safeName)
    }

    fun appendQrNotice(body: String): String {
        val newline = detectNewline(body)
        val trimmed = body.trimEnd('\n', '\r')
        return if (trimmed.isEmpty()) {
            QR_ATTACHMENT_NOTICE
        } else {
            trimmed + newline + newline + QR_ATTACHMENT_NOTICE
        }
    }

    fun toHtml(body: String): String {
        val escaped = escapeHtml(body)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        val withBreaks = escaped.replace("\n", "<br/>\n")
        val noticeHtml = "<strong>${escapeHtml(QR_ATTACHMENT_NOTICE)}</strong>"
        val htmlBody = withBreaks.replace(escapeHtml(QR_ATTACHMENT_NOTICE), noticeHtml)
        return """
<html>
<body style="font-family: Arial, Helvetica, sans-serif; color: #222; line-height: 1.5;">
$htmlBody
</body>
</html>
        """.trimIndent()
    }

    private data class LineBreak(val lineEnd: Int, val nextContent: Int)

    private fun firstLineBreak(content: String): LineBreak? {
        var i = 0
        while (i < content.length) {
            val ch = content[i]
            if (ch == '\n') {
                return LineBreak(i, i + 1)
            }
            if (ch == '\r') {
                val next = if (i + 1 < content.length && content[i + 1] == '\n') i + 2 else i + 1
                return LineBreak(i, next)
            }
            i++
        }
        return null
    }

    private fun detectNewline(body: String): String {
        return when {
            body.contains("\r\n") -> "\r\n"
            body.contains("\r") -> "\r"
            else -> "\n"
        }
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
