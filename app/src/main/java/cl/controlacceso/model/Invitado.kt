package cl.controlacceso.model

data class Invitado(
    val pmiId: String,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val horaEntrada: Long? = null,
    val ingresoRegistrado: Boolean = false
) {
    val nombreCompleto: String
        get() = "$nombre $apellidos".trim()
}
