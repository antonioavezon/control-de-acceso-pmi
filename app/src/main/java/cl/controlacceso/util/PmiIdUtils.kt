package cl.controlacceso.util

object PmiIdUtils {

    private val ONLY_DIGITS = Regex("^\\d+$")

    fun normalize(raw: String?): String? {
        if (raw == null) return null
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        if (!ONLY_DIGITS.matches(cleaned)) return null
        return cleaned
    }

    fun isValid(raw: String?): Boolean = normalize(raw) != null
}
