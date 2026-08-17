package cl.controlacceso.parser

import cl.controlacceso.model.CsvImportResult
import cl.controlacceso.model.Invitado
import cl.controlacceso.util.EmailUtils
import cl.controlacceso.util.PmiIdUtils

object CsvParser {

    private val EXPECTED_HEADERS = listOf("NOMBRE", "APELLIDOS", "PMIID", "EMAIL")

    fun parse(content: String?): CsvImportResult {
        if (content == null) {
            return CsvImportResult.Failure("No fue posible leer el archivo seleccionado.")
        }

        val lines = content.lines()
        val nonEmptyLines = lines.mapIndexed { index, line -> index to line }
            .filter { (_, line) -> line.trim().isNotEmpty() }

        if (nonEmptyLines.isEmpty()) {
            return CsvImportResult.Failure("El archivo no contiene datos.")
        }

        val firstLine = nonEmptyLines.first().second.trim()
        val delimiter = detectDelimiter(firstLine)
            ?: return CsvImportResult.Failure(
                "El archivo no tiene el formato esperado.\nSe esperaba el orden: NOMBRE, APELLIDOS, PMIID, EMAIL."
            )

        val firstFields = splitCsvLine(firstLine, delimiter)
        val hasHeader = looksLikeHeader(firstFields)

        val columnMap: Map<String, Int>
        val dataRows: List<Pair<Int, String>>

        if (hasHeader) {
            val normalized = firstFields.map { normalizeHeader(it) }
            val missing = EXPECTED_HEADERS.filter { required ->
                normalized.none { it == required || aliases(required).contains(it) }
            }
            if (missing.isNotEmpty()) {
                return CsvImportResult.Failure(
                    "El archivo no tiene el formato esperado.\nSe esperaba el orden: NOMBRE, APELLIDOS, PMIID, EMAIL."
                )
            }
            columnMap = buildColumnMap(normalized)
            dataRows = nonEmptyLines.drop(1)
        } else {
            if (firstFields.size < 4) {
                return CsvImportResult.Failure(
                    "El archivo no tiene el formato esperado.\nSe esperaba el orden: NOMBRE, APELLIDOS, PMIID, EMAIL."
                )
            }
            columnMap = mapOf(
                "NOMBRE" to 0,
                "APELLIDOS" to 1,
                "PMIID" to 2,
                "EMAIL" to 3
            )
            dataRows = nonEmptyLines
        }

        if (dataRows.isEmpty()) {
            return CsvImportResult.Failure("El archivo no contiene datos.")
        }

        val invitados = mutableListOf<Invitado>()
        val seenPmi = mutableSetOf<String>()

        for ((lineIndex, rawLine) in dataRows) {
            val rowNumber = lineIndex + 1
            val fields = splitCsvLine(rawLine.trim(), delimiter)

            if (fields.all { it.isBlank() }) {
                continue
            }

            if (fields.size < 4) {
                return CsvImportResult.Failure(
                    "La fila $rowNumber no tiene el formato esperado.\nSe esperaba el orden: NOMBRE, APELLIDOS, PMIID, EMAIL."
                )
            }

            val nombre = fields.getOrNull(columnMap.getValue("NOMBRE"))?.trim().orEmpty()
            val apellidos = fields.getOrNull(columnMap.getValue("APELLIDOS"))?.trim().orEmpty()
            val pmiRaw = fields.getOrNull(columnMap.getValue("PMIID"))?.trim().orEmpty()
            val email = fields.getOrNull(columnMap.getValue("EMAIL"))?.trim().orEmpty()

            if (nombre.isEmpty()) {
                return CsvImportResult.Failure("La fila $rowNumber tiene el nombre vacío.")
            }
            if (apellidos.isEmpty()) {
                return CsvImportResult.Failure("La fila $rowNumber tiene los apellidos vacíos.")
            }
            if (pmiRaw.isEmpty()) {
                return CsvImportResult.Failure("La fila $rowNumber tiene el PMI ID vacío.")
            }
            val pmiId = PmiIdUtils.normalize(pmiRaw)
            if (pmiId == null) {
                return CsvImportResult.Failure("La fila $rowNumber: El PMI ID debe contener solo números.")
            }
            if (!seenPmi.add(pmiId)) {
                return CsvImportResult.Failure("Se encontraron PMI ID duplicados. ($pmiId)")
            }
            if (!EmailUtils.isValid(email)) {
                return CsvImportResult.Failure("La fila $rowNumber tiene un correo electrónico inválido.")
            }

            invitados.add(
                Invitado(
                    pmiId = pmiId,
                    nombre = nombre,
                    apellidos = apellidos,
                    email = email
                )
            )
        }

        if (invitados.isEmpty()) {
            return CsvImportResult.Failure("El archivo no contiene datos.")
        }

        return CsvImportResult.Success(invitados)
    }

    private fun detectDelimiter(line: String): Char? {
        val pipeCount = line.count { it == '|' }
        val commaCount = countUnquotedCommas(line)
        return when {
            pipeCount >= 3 -> '|'
            commaCount >= 3 -> ','
            else -> null
        }
    }

    private fun countUnquotedCommas(line: String): Int {
        var count = 0
        var inQuotes = false
        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) count++
            }
        }
        return count
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun looksLikeHeader(fields: List<String>): Boolean {
        val normalized = fields.map { normalizeHeader(it) }
        return normalized.any { it in EXPECTED_HEADERS || aliases("EMAIL").contains(it) || it == "PMI_ID" || it == "MAIL" }
    }

    private fun normalizeHeader(value: String): String {
        return value.trim()
            .uppercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
    }

    private fun aliases(header: String): Set<String> {
        return when (header) {
            "PMIID" -> setOf("PMIID", "PMI", "PMID")
            "EMAIL" -> setOf("EMAIL", "MAIL", "CORREO", "CORREOELECTRONICO")
            "NOMBRE" -> setOf("NOMBRE", "NAME", "FIRSTNAME")
            "APELLIDOS" -> setOf("APELLIDOS", "APELLIDO", "LASTNAME", "SURNAME")
            else -> setOf(header)
        }
    }

    private fun buildColumnMap(normalizedHeaders: List<String>): Map<String, Int> {
        fun find(required: String): Int {
            val wanted = aliases(required).map { normalizeHeader(it) }.toSet()
            return normalizedHeaders.indexOfFirst { normalizeHeader(it) in wanted }
        }
        return mapOf(
            "NOMBRE" to find("NOMBRE"),
            "APELLIDOS" to find("APELLIDOS"),
            "PMIID" to find("PMIID"),
            "EMAIL" to find("EMAIL")
        )
    }
}
