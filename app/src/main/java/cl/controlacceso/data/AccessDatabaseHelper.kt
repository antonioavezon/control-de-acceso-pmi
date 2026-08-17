package cl.controlacceso.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Esquema PMI ID (v2).
 *
 * La versión 1 almacenaba ingresos por RUT chileno. Esa estructura es incompatible
 * con el nuevo modelo (PMIID / nombre / apellidos / email), por lo que la migración
 * recrea las tablas. Los archivos CSV exportados previamente NO se eliminan.
 */
class AccessDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PARTICIPANTES (
                pmi_id TEXT PRIMARY KEY NOT NULL,
                nombre TEXT NOT NULL,
                apellidos TEXT NOT NULL,
                email TEXT NOT NULL,
                hora_entrada INTEGER,
                ingreso_registrado INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENT_META (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                resumen_exportado INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO $TABLE_EVENT_META (id, resumen_exportado) VALUES (1, 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Migrando DB de v$oldVersion a v$newVersion: recreando esquema PMI (sin tocar archivos exportados).")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INGRESOS_LEGACY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PARTICIPANTES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENT_META")
        onCreate(db)
    }

    companion object {
        private const val TAG = "AccessDatabaseHelper"
        const val DATABASE_NAME = "control_acceso.db"
        const val DATABASE_VERSION = 2

        const val TABLE_PARTICIPANTES = "participantes"
        const val TABLE_EVENT_META = "event_meta"

        /** Tabla legacy v1 (RUT). */
        const val TABLE_INGRESOS_LEGACY = "ingresos"
    }
}
