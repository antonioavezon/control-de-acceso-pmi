package cl.controlacceso.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import cl.controlacceso.model.EmailTemplate
import cl.controlacceso.model.Invitado
import cl.controlacceso.parser.EmailTemplateParser
import java.io.File
import java.io.FileOutputStream

object InvitationMailer {

    private const val TAG = "InvitationMailer"

    data class PrepareResult(
        val success: Boolean,
        val intent: Intent? = null,
        val errorMessage: String? = null
    )

    fun validateParticipant(invitado: Invitado): String? {
        if (invitado.nombre.isBlank()) {
            return "El participante no tiene nombre. No se puede generar la invitación."
        }
        if (invitado.email.isBlank() || !EmailUtils.isValid(invitado.email)) {
            return "El participante no tiene un correo válido. No se puede generar la invitación."
        }
        if (!PmiIdUtils.isValid(invitado.pmiId)) {
            return "El participante no tiene un PMI ID válido. No se puede generar la invitación."
        }
        return null
    }

    fun prepareInvitation(
        context: Context,
        invitado: Invitado,
        template: EmailTemplate = EmailTemplateParser.defaultTemplate()
    ): PrepareResult {
        validateParticipant(invitado)?.let {
            return PrepareResult(success = false, errorMessage = it)
        }

        val qrBitmap = QrGenerator.generateBitmap(invitado.pmiId)
            ?: return PrepareResult(
                success = false,
                errorMessage = "No fue posible generar el código QR del participante."
            )

        val qrUri = try {
            saveQrToCache(context, invitado.pmiId, qrBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando QR", e)
            return PrepareResult(
                success = false,
                errorMessage = "No fue posible preparar el archivo del código QR."
            )
        }

        val personalized = EmailTemplateParser.personalize(template.body, invitado.nombre)
        val body = EmailTemplateParser.appendQrNotice(personalized)
        val htmlBody = EmailTemplateParser.toHtml(body)
        val subject = EmailTemplateParser.personalize(template.subject, invitado.nombre)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(invitado.email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_HTML_TEXT, htmlBody)
            putExtra(Intent.EXTRA_STREAM, qrUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(
                context.contentResolver,
                "qr",
                qrUri
            )
        }

        return PrepareResult(success = true, intent = Intent.createChooser(intent, "Enviar invitación"))
    }

    private fun saveQrToCache(context: Context, pmiId: String, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "invitations").apply { mkdirs() }
        val file = File(dir, "qr_$pmiId.png")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw IllegalStateException("compress failed")
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
