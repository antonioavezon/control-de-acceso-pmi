package cl.controlacceso.data

import android.content.ContentValues
import android.content.Context
import android.util.Log
import cl.controlacceso.model.Invitado
import cl.controlacceso.model.ScanResult
import cl.controlacceso.util.PmiIdUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dbHelper = AccessDatabaseHelper(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val totalInvitados: Int
        get() = countWhere(null)

    val totalAsistentes: Int
        get() = countWhere("ingreso_registrado = 1")

    val totalSinIngresar: Int
        get() = (totalInvitados - totalAsistentes).coerceAtLeast(0)

    fun hasGuestList(): Boolean = totalInvitados > 0

    fun getCsvUri(): String? = prefs.getString(KEY_CSV_URI, null)

    fun isResumenExportado(): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AccessDatabaseHelper.TABLE_EVENT_META,
            arrayOf("resumen_exportado"),
            "id = 1",
            null,
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) == 1 else false
        }
    }

    fun markResumenExportado() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("resumen_exportado", 1) }
        db.update(AccessDatabaseHelper.TABLE_EVENT_META, values, "id = 1", null)
    }

    fun getInvitado(pmiId: String): Invitado? {
        val id = PmiIdUtils.normalize(pmiId) ?: return null
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AccessDatabaseHelper.TABLE_PARTICIPANTES,
            COLUMNS,
            "pmi_id = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToInvitado(it) else null
        }
    }

    fun getAllInvitados(): List<Invitado> {
        val result = mutableListOf<Invitado>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AccessDatabaseHelper.TABLE_PARTICIPANTES,
            COLUMNS,
            null,
            null,
            null,
            null,
            "nombre ASC, apellidos ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(cursorToInvitado(it))
            }
        }
        return result
    }

    fun getAsistentes(): List<Invitado> {
        val result = mutableListOf<Invitado>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AccessDatabaseHelper.TABLE_PARTICIPANTES,
            COLUMNS,
            "ingreso_registrado = 1",
            null,
            null,
            null,
            "hora_entrada ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(cursorToInvitado(it))
            }
        }
        return result
    }

    fun saveGuestList(invitados: List<Invitado>, csvUri: String?) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(AccessDatabaseHelper.TABLE_PARTICIPANTES, null, null)
            invitados.forEach { invitado ->
                val values = ContentValues().apply {
                    put("pmi_id", invitado.pmiId)
                    put("nombre", invitado.nombre)
                    put("apellidos", invitado.apellidos)
                    put("email", invitado.email)
                    putNull("hora_entrada")
                    put("ingreso_registrado", 0)
                }
                db.insertOrThrow(AccessDatabaseHelper.TABLE_PARTICIPANTES, null, values)
            }
            // Nueva lista = nuevo ciclo de evento: resetear flag de exportación.
            val meta = ContentValues().apply { put("resumen_exportado", 0) }
            db.update(AccessDatabaseHelper.TABLE_EVENT_META, meta, "id = 1", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        prefs.edit().apply {
            if (csvUri != null) putString(KEY_CSV_URI, csvUri)
            apply()
        }
    }

    /**
     * Procesa un escaneo de QR. El primer ingreso guarda HORA_ENTRADA de forma inmutable.
     * Reingresos: PERMITIDO sin alterar hora ni contador de únicos.
     */
    fun processScan(rawQr: String): ScanResult {
        val pmiId = cl.controlacceso.parser.QrParser.parsePmiId(rawQr)
        if (pmiId == null) {
            return ScanResult.NoPermitido(
                motivo = "QR inválido o no interpretable."
            )
        }

        val invitado = getInvitado(pmiId)
        if (invitado == null) {
            return ScanResult.NoPermitido(
                motivo = "PMI ID no encontrado en la lista de invitados.",
                pmiIdDetectado = pmiId
            )
        }

        if (invitado.ingresoRegistrado && invitado.horaEntrada != null) {
            return ScanResult.Permitido(
                pmiId = invitado.pmiId,
                nombreCompleto = invitado.nombreCompleto,
                email = invitado.email,
                horaEntrada = formatHora(invitado.horaEntrada),
                esReingreso = true
            )
        }

        val now = System.currentTimeMillis()
        val registered = registrarPrimerIngreso(invitado.pmiId, now)
        if (!registered) {
            // Condición de carrera improbable: otro hilo registró entre lectura e insert.
            val refreshed = getInvitado(invitado.pmiId)
            if (refreshed?.ingresoRegistrado == true && refreshed.horaEntrada != null) {
                return ScanResult.Permitido(
                    pmiId = refreshed.pmiId,
                    nombreCompleto = refreshed.nombreCompleto,
                    email = refreshed.email,
                    horaEntrada = formatHora(refreshed.horaEntrada),
                    esReingreso = true
                )
            }
            Log.e(TAG, "No se pudo registrar primer ingreso para PMI ${invitado.pmiId}")
            return ScanResult.NoPermitido(
                motivo = "No fue posible registrar el ingreso. Intente nuevamente.",
                pmiIdDetectado = invitado.pmiId
            )
        }

        return ScanResult.Permitido(
            pmiId = invitado.pmiId,
            nombreCompleto = invitado.nombreCompleto,
            email = invitado.email,
            horaEntrada = formatHora(now),
            esReingreso = false
        )
    }

    /**
     * Registra el primer ingreso solo si aún no existe.
     * Usa UPDATE condicional para no sobrescribir HORA_ENTRADA.
     */
    fun registrarPrimerIngreso(pmiId: String, timestamp: Long = System.currentTimeMillis()): Boolean {
        val id = PmiIdUtils.normalize(pmiId) ?: return false
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("hora_entrada", timestamp)
            put("ingreso_registrado", 1)
        }
        val updated = db.update(
            AccessDatabaseHelper.TABLE_PARTICIPANTES,
            values,
            "pmi_id = ? AND ingreso_registrado = 0",
            arrayOf(id)
        )
        return updated > 0
    }

    fun formatHora(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("PMIID,NOMBRE,APELLIDOS,MAIL,HORAENTRADA")
        getAsistentes().forEach { invitado ->
            val hora = invitado.horaEntrada?.let { formatHora(it) }.orEmpty()
            sb.appendLine(
                listOf(
                    invitado.pmiId,
                    escapeCsv(invitado.nombre),
                    escapeCsv(invitado.apellidos),
                    escapeCsv(invitado.email),
                    hora
                ).joinToString(",")
            )
        }
        sb.appendLine("TOTAL ASISTENTES: $totalAsistentes")
        return sb.toString()
    }

    /**
     * Elimina participantes y metadatos del evento en SQLite.
     * No elimina archivos exportados del almacenamiento del dispositivo.
     */
    fun clearEventData(): Boolean {
        if (!isResumenExportado()) return false
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(AccessDatabaseHelper.TABLE_PARTICIPANTES, null, null)
            val meta = ContentValues().apply { put("resumen_exportado", 0) }
            db.update(AccessDatabaseHelper.TABLE_EVENT_META, meta, "id = 1", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        prefs.edit().remove(KEY_CSV_URI).apply()
        return true
    }

    private fun countWhere(where: String?): Int {
        val db = dbHelper.readableDatabase
        val sql = if (where.isNullOrBlank()) {
            "SELECT COUNT(*) FROM ${AccessDatabaseHelper.TABLE_PARTICIPANTES}"
        } else {
            "SELECT COUNT(*) FROM ${AccessDatabaseHelper.TABLE_PARTICIPANTES} WHERE $where"
        }
        val cursor = db.rawQuery(sql, null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun cursorToInvitado(cursor: android.database.Cursor): Invitado {
        val horaIndex = cursor.getColumnIndexOrThrow("hora_entrada")
        val hora = if (cursor.isNull(horaIndex)) null else cursor.getLong(horaIndex)
        return Invitado(
            pmiId = cursor.getString(cursor.getColumnIndexOrThrow("pmi_id")),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
            apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
            email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
            horaEntrada = hora,
            ingresoRegistrado = cursor.getInt(cursor.getColumnIndexOrThrow("ingreso_registrado")) == 1
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    companion object {
        private const val TAG = "EventRepository"
        private const val PREFS_NAME = "control_acceso_prefs"
        private const val KEY_CSV_URI = "csv_uri"
        private val COLUMNS = arrayOf(
            "pmi_id", "nombre", "apellidos", "email", "hora_entrada", "ingreso_registrado"
        )
    }
}
