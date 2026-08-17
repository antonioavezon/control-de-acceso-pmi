package cl.controlacceso.parser

import cl.controlacceso.model.CsvImportResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserTest {

    @Test
    fun parsesPipeDelimitedWithHeader() {
        val csv = """
            NOMBRE | APELLIDOS | PMIID | EMAIL
            Maria | Perez | 1234567 | maria.perez@ejemplo.com
        """.trimIndent()

        val result = CsvParser.parse(csv)
        assertTrue(result is CsvImportResult.Success)
        val list = (result as CsvImportResult.Success).invitados
        assertEquals(1, list.size)
        assertEquals("1234567", list[0].pmiId)
        assertEquals("Maria", list[0].nombre)
        assertEquals("Perez", list[0].apellidos)
    }

    @Test
    fun rejectsLettersInPmiId() {
        val csv = """
            NOMBRE | APELLIDOS | PMIID | EMAIL
            Maria | Perez | 11260A87 | maria.perez@ejemplo.com
        """.trimIndent()
        val result = CsvParser.parse(csv)
        assertTrue(result is CsvImportResult.Failure)
        assertTrue((result as CsvImportResult.Failure).message.contains("solo números"))
    }

    @Test
    fun rejectsDuplicatePmiId() {
        val csv = """
            NOMBRE | APELLIDOS | PMIID | EMAIL
            Maria | Perez | 1234567 | a@test.cl
            Maria | Soto | 1234567 | b@test.cl
        """.trimIndent()
        val result = CsvParser.parse(csv)
        assertTrue(result is CsvImportResult.Failure)
        assertTrue((result as CsvImportResult.Failure).message.contains("duplicados"))
    }

    @Test
    fun rejectsInvalidEmailWithRow() {
        val csv = """
            NOMBRE | APELLIDOS | PMIID | EMAIL
            Maria | Perez | 1234567 | no-es-email
        """.trimIndent()
        val result = CsvParser.parse(csv)
        assertTrue(result is CsvImportResult.Failure)
        assertTrue((result as CsvImportResult.Failure).message.contains("fila 2"))
        assertTrue(result.message.contains("correo"))
    }

    @Test
    fun rejectsEmptyFile() {
        val result = CsvParser.parse("\n\n")
        assertTrue(result is CsvImportResult.Failure)
        assertTrue((result as CsvImportResult.Failure).message.contains("no contiene datos"))
    }

    @Test
    fun rejectsWrongColumns() {
        val byHeader = CsvParser.parse(
            """
            PMIID | EMAIL | NOMBRE | APELLIDOS
            1234567 | a@test.cl | Maria | Perez
            """.trimIndent()
        )
        // Encabezados inequívocos: se importa por nombre de columna.
        assertTrue(byHeader is CsvImportResult.Success)

        val wrong = CsvParser.parse("col1,col2\nvalor1,valor2")
        assertTrue(wrong is CsvImportResult.Failure)
        assertTrue((wrong as CsvImportResult.Failure).message.contains("formato esperado"))
    }

    @Test
    fun parsesReorderedHeadersReliably() {
        val csv = """
            EMAIL,PMIID,APELLIDOS,NOMBRE
            maria.perez@ejemplo.com,1234567,Perez,Maria
        """.trimIndent()
        val result = CsvParser.parse(csv)
        assertTrue(result is CsvImportResult.Success)
        val inv = (result as CsvImportResult.Success).invitados.first()
        assertEquals("Maria", inv.nombre)
        assertEquals("Perez", inv.apellidos)
        assertEquals("1234567", inv.pmiId)
    }
}
