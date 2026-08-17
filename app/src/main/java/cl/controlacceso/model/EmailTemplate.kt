package cl.controlacceso.model

data class EmailTemplate(
    val subject: String,
    val body: String,
    val fromFile: Boolean
)
