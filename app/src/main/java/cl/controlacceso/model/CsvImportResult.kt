package cl.controlacceso.model

sealed class CsvImportResult {
    data class Success(val invitados: List<Invitado>) : CsvImportResult()
    data class Failure(val message: String) : CsvImportResult()
}
