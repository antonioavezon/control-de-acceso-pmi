package cl.controlacceso

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cl.controlacceso.data.EmailTemplateRepository
import cl.controlacceso.data.EventRepository
import cl.controlacceso.databinding.ActivityHomeBinding
import cl.controlacceso.model.CsvImportResult
import cl.controlacceso.parser.CsvParser
import cl.controlacceso.parser.EmailTemplateParseResult

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var eventRepository: EventRepository
    private lateinit var emailTemplateRepository: EmailTemplateRepository

    private val openDocumentLauncher = registerForActivityResult(OpenTextDocument()) { uri ->
        if (uri != null) {
            loadCsvFromUri(uri)
        }
    }

    private val openEmailLauncher = registerForActivityResult(OpenTextDocument()) { uri ->
        if (uri != null) {
            loadEmailFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cl.controlacceso.util.UiHelpers.liftContentAboveNavBar(binding.root)

        val app = application as ControlAccesoApp
        eventRepository = app.eventRepository
        emailTemplateRepository = app.emailTemplateRepository

        binding.loadListButton.setOnClickListener { openDocumentLauncher.launch(Unit) }
        binding.viewParticipantsButton.setOnClickListener {
            startActivity(Intent(this, ParticipantsActivity::class.java))
        }
        binding.loadEmailButton.setOnClickListener { openEmailLauncher.launch(Unit) }
        binding.invitationsButton.setOnClickListener {
            startActivity(Intent(this, InvitesActivity::class.java))
        }
        binding.accessButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.summaryButton.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val total = eventRepository.totalInvitados
        binding.guestSummaryText.text = if (total == 0) {
            getString(R.string.no_guest_list)
        } else {
            getString(R.string.guests_loaded, total)
        }
        binding.viewParticipantsButton.isEnabled = eventRepository.hasGuestList()
        binding.emailSummaryText.text = if (emailTemplateRepository.hasCustomTemplate()) {
            getString(R.string.email_template_loaded)
        } else {
            getString(R.string.email_template_default)
        }
    }

    private fun loadCsvFromUri(uri: Uri) {
        try {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "No se persistió el permiso del CSV", e)
            }

            val stream = contentResolver.openInputStream(uri)
            if (stream == null) {
                showMessage(getString(R.string.csv_file_not_found))
                return
            }

            val content = stream.bufferedReader().use { it.readText() }
            when (val result = CsvParser.parse(content)) {
                is CsvImportResult.Success -> {
                    val applyImport = {
                        eventRepository.saveGuestList(result.invitados, uri.toString())
                        refreshUi()
                        showMessage(getString(R.string.guests_loaded, result.invitados.size))
                    }
                    if (eventRepository.totalAsistentes > 0 || eventRepository.hasGuestList()) {
                        AlertDialog.Builder(this)
                            .setTitle(R.string.reload_list_warning_title)
                            .setMessage(R.string.reload_list_warning_message)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.reload_list_confirm) { _, _ -> applyImport() }
                            .show()
                    } else {
                        applyImport()
                    }
                }
                is CsvImportResult.Failure -> showMessage(result.message)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso para leer CSV", e)
            showMessage(getString(R.string.csv_reload_error))
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo CSV", e)
            showMessage(getString(R.string.csv_read_error))
        }
    }

    private fun loadEmailFromUri(uri: Uri) {
        try {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "No se persistió el permiso del archivo de correo", e)
            }
            val stream = contentResolver.openInputStream(uri)
            if (stream == null) {
                showMessage(getString(R.string.email_file_not_found))
                return
            }
            val content = stream.bufferedReader().use { it.readText() }
            when (val result = emailTemplateRepository.saveTemplate(content, uri.toString())) {
                is EmailTemplateParseResult.Success -> {
                    refreshUi()
                    showMessage(getString(R.string.email_template_loaded_ok))
                }
                is EmailTemplateParseResult.Failure -> showMessage(result.message)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso para leer email.txt", e)
            showMessage(getString(R.string.email_read_error))
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo email.txt", e)
            showMessage(getString(R.string.email_read_error))
        }
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private class OpenTextDocument : ActivityResultContract<Unit, Uri?>() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                cl.controlacceso.util.UiHelpers.applyOpenDocumentDefaults(this)
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "text/*",
                        "text/plain",
                        "text/comma-separated-values",
                        "text/csv"
                    )
                )
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode != Activity.RESULT_OK || intent == null) null else intent.data
        }
    }

    companion object {
        private const val TAG = "HomeActivity"
    }
}
