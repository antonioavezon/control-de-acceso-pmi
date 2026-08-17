package cl.controlacceso.parser

import cl.controlacceso.util.PmiIdUtils

/**
 * El QR de invitación contiene preferentemente solo el PMI ID (dígitos).
 * También se acepta el prefijo opcional "PMI:" para payloads generados por la app.
 */
object QrParser {

    fun parsePmiId(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()

        val candidate = when {
            trimmed.startsWith("PMI:", ignoreCase = true) ->
                trimmed.substringAfter(':').trim()
            trimmed.startsWith("PMI|", ignoreCase = true) ->
                trimmed.substringAfter('|').trim()
            else -> trimmed
        }

        // Rechazar payloads con saltos de línea / URLs de cédula / texto extra.
        if (candidate.contains('\n') || candidate.contains(' ') || candidate.contains('/')) {
            // Si es una URL u otro formato, intentar extraer solo dígitos no es seguro.
            return null
        }

        return PmiIdUtils.normalize(candidate)
    }

    /** Payload embebido en el QR generado para invitaciones. */
    fun buildPayload(pmiId: String): String {
        val normalized = PmiIdUtils.normalize(pmiId)
            ?: throw IllegalArgumentException("PMI ID inválido")
        return normalized
    }
}
