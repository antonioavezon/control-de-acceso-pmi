package cl.controlacceso.data

import android.content.Context
import android.util.Log
import cl.controlacceso.model.EmailTemplate
import cl.controlacceso.parser.EmailTemplateParseResult
import cl.controlacceso.parser.EmailTemplateParser
import java.io.File

class EmailTemplateRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val templateFile = File(appContext.filesDir, TEMPLATE_FILE)

    fun hasCustomTemplate(): Boolean = templateFile.exists() && templateFile.length() > 0L

    fun getEmailUri(): String? = prefs.getString(KEY_EMAIL_URI, null)

    fun getTemplate(): EmailTemplate {
        if (!hasCustomTemplate()) {
            return EmailTemplateParser.defaultTemplate()
        }
        return try {
            when (val parsed = EmailTemplateParser.parse(templateFile.readText())) {
                is EmailTemplateParseResult.Success -> parsed.template
                is EmailTemplateParseResult.Failure -> {
                    Log.w(TAG, "Plantilla de correo inválida en disco: ${parsed.message}")
                    EmailTemplateParser.defaultTemplate()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo plantilla de correo", e)
            EmailTemplateParser.defaultTemplate()
        }
    }

    fun saveTemplate(rawContent: String, sourceUri: String?): EmailTemplateParseResult {
        val parsed = EmailTemplateParser.parse(rawContent)
        if (parsed is EmailTemplateParseResult.Success) {
            try {
                templateFile.writeText(rawContent)
                prefs.edit().apply {
                    if (sourceUri != null) putString(KEY_EMAIL_URI, sourceUri)
                    apply()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando plantilla de correo", e)
                return EmailTemplateParseResult.Failure("No fue posible guardar el archivo de correo.")
            }
        }
        return parsed
    }

    companion object {
        private const val TAG = "EmailTemplateRepository"
        private const val PREFS_NAME = "control_acceso_prefs"
        private const val KEY_EMAIL_URI = "email_uri"
        private const val TEMPLATE_FILE = "email_template.txt"
    }
}
