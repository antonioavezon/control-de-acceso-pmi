package cl.controlacceso

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cl.controlacceso.data.EventRepository
import cl.controlacceso.databinding.ActivitySummaryBinding
import cl.controlacceso.model.Invitado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private lateinit var eventRepository: EventRepository
    private var csvContent: String = ""

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        if (uri != null) {
            saveCsvToUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cl.controlacceso.util.UiHelpers.liftContentAboveNavBar(binding.root)

        eventRepository = (application as ControlAccesoApp).eventRepository

        binding.backButton.setOnClickListener { finish() }
        binding.exportCsvButton.setOnClickListener { exportSummary() }
        binding.deleteDataButton.setOnClickListener { onDeleteDataClicked() }

        displaySummary()
    }

    override fun onResume() {
        super.onResume()
        displaySummary()
    }

    private fun displaySummary() {
        val invitados = eventRepository.totalInvitados
        val asistentes = eventRepository.totalAsistentes
        val pendientes = eventRepository.totalSinIngresar

        binding.summaryInvitedText.text = getString(R.string.summary_invited, invitados)
        binding.summaryAttendedText.text = getString(R.string.summary_attended, asistentes)
        binding.summaryPendingText.text = getString(R.string.summary_pending, pendientes)
        binding.summaryExportFlagText.text = if (eventRepository.isResumenExportado()) {
            getString(R.string.summary_exported_yes)
        } else {
            getString(R.string.summary_exported_no)
        }

        val lista = eventRepository.getAsistentes()
        csvContent = eventRepository.exportCsv()
        binding.exportCsvButton.isEnabled = true

        if (lista.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.entriesList.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.entriesList.visibility = View.VISIBLE
            binding.entriesList.adapter = AsistenteAdapter(lista)
        }
    }

    private fun exportSummary() {
        try {
            csvContent = eventRepository.exportCsv()
            val fileName = "resumen_asistencia_${fileNameDateFormat.format(Date())}.csv"
            createDocumentLauncher.launch(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparando exportación", e)
            Toast.makeText(this, R.string.export_entries_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveCsvToUri(uri: android.net.Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(csvContent.toByteArray(Charsets.UTF_8))
            } ?: run {
                Toast.makeText(this, R.string.export_entries_error, Toast.LENGTH_LONG).show()
                return
            }
            eventRepository.markResumenExportado()
            displaySummary()
            Toast.makeText(this, R.string.export_entries_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando CSV", e)
            Toast.makeText(this, R.string.export_entries_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun onDeleteDataClicked() {
        if (!eventRepository.isResumenExportado()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_data_blocked)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.delete_data_title)
            .setMessage(R.string.delete_data_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_data_confirm) { _, _ ->
                performDelete()
            }
            .show()
    }

    private fun performDelete() {
        try {
            val ok = eventRepository.clearEventData()
            if (!ok) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.delete_data_blocked)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return
            }
            displaySummary()
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_data_success)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando datos", e)
            Toast.makeText(this, "No fue posible eliminar los datos del evento.", Toast.LENGTH_LONG).show()
        }
    }

    private inner class AsistenteAdapter(
        private val items: List<Invitado>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Invitado = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@SummaryActivity)
                .inflate(R.layout.item_ingreso, parent, false)

            val item = items[position]
            view.findViewById<TextView>(R.id.pmiText).text = item.pmiId
            view.findViewById<TextView>(R.id.nombreText).text = item.nombreCompleto
            view.findViewById<TextView>(R.id.emailText).text = item.email
            view.findViewById<TextView>(R.id.horaText).text =
                item.horaEntrada?.let { eventRepository.formatHora(it) }.orEmpty()
            return view
        }
    }

    companion object {
        private const val TAG = "SummaryActivity"
        private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
    }
}
