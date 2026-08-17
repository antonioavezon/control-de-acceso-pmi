package cl.controlacceso.util

object EmailUtils {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    fun isValid(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val trimmed = email.trim()
        if (trimmed.length > 254) return false
        return EMAIL_REGEX.matches(trimmed)
    }
}
