package cl.controlacceso.model

sealed class ScanResult {
    data class Permitido(
        val pmiId: String,
        val nombreCompleto: String,
        val email: String,
        val horaEntrada: String,
        val esReingreso: Boolean
    ) : ScanResult()

    data class NoPermitido(
        val motivo: String,
        val pmiIdDetectado: String? = null
    ) : ScanResult()
}
