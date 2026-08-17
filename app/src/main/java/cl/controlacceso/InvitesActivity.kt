package cl.controlacceso

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cl.controlacceso.data.EmailTemplateRepository
import cl.controlacceso.data.EventRepository
import cl.controlacceso.databinding.ActivityInvitesBinding
import cl.controlacceso.model.EmailTemplate
import cl.controlacceso.model.Invitado
import cl.controlacceso.util.InvitationMailer

class InvitesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvitesBinding
    private lateinit var eventRepository: EventRepository
    private lateinit var emailTemplateRepository: EmailTemplateRepository

    private val inviteQueue = ArrayDeque<Invitado>()
    private var inviteTotal = 0
    private var skippedCount = 0
    private var sendingBulk = false

    private val sendLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (sendingBulk) {
            sendNextFromQueue()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cl.controlacceso.util.UiHelpers.liftContentAboveNavBar(binding.root)

        val app = application as ControlAccesoApp
        eventRepository = app.eventRepository
        emailTemplateRepository = app.emailTemplateRepository

        binding.backButton.setOnClickListener { finish() }
        binding.inviteAllButton.setOnClickListener { confirmInviteAll() }
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        if (!sendingBulk) {
            refreshList()
        }
    }

    private fun refreshList() {
        val participantes = eventRepository.getAllInvitados()
        binding.inviteAllButton.isEnabled = participantes.isNotEmpty() && !sendingBulk
        if (participantes.isEmpty()) {
            binding.emptyInvitesText.visibility = View.VISIBLE
            binding.participantsList.visibility = View.GONE
            return
        }
        binding.emptyInvitesText.visibility = View.GONE
        binding.participantsList.visibility = View.VISIBLE
        binding.participantsList.adapter = InviteAdapter(participantes)
    }

    private fun currentTemplate(): EmailTemplate = emailTemplateRepository.getTemplate()

    private fun confirmInviteAll() {
        val participantes = eventRepository.getAllInvitados()
        if (participantes.isEmpty()) {
            showMessage(getString(R.string.no_participants_for_invites))
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.invite_all_confirm_title)
            .setMessage(getString(R.string.invite_all_confirm_message, participantes.size))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.invite_all) { _, _ ->
                startInviteAll(participantes)
            }
            .show()
    }

    private fun startInviteAll(participantes: List<Invitado>) {
        inviteQueue.clear()
        inviteQueue.addAll(participantes)
        inviteTotal = participantes.size
        skippedCount = 0
        sendingBulk = true
        binding.inviteAllButton.isEnabled = false
        sendNextFromQueue()
    }

    private fun sendNextFromQueue() {
        val next = inviteQueue.removeFirstOrNull()
        if (next == null) {
            finishBulk()
            return
        }
        val remainingDone = inviteTotal - inviteQueue.size
        binding.invitesHint.text = getString(R.string.invite_all_progress, remainingDone, inviteTotal)
        if (!openInvitation(next, fromQueue = true)) {
            skippedCount++
            sendNextFromQueue()
        }
    }

    private fun finishBulk() {
        sendingBulk = false
        binding.inviteAllButton.isEnabled = eventRepository.hasGuestList()
        binding.invitesHint.text = getString(R.string.invite_sent_hint)
        val message = if (skippedCount > 0) {
            getString(R.string.invite_all_partial, skippedCount)
        } else {
            getString(R.string.invite_all_done)
        }
        showMessage(message)
        refreshList()
    }

    private fun sendInvitation(invitado: Invitado) {
        openInvitation(invitado, fromQueue = false)
    }

    private fun openInvitation(invitado: Invitado, fromQueue: Boolean): Boolean {
        return try {
            val result = InvitationMailer.prepareInvitation(this, invitado, currentTemplate())
            if (!result.success || result.intent == null) {
                if (!fromQueue) {
                    showMessage(result.errorMessage ?: getString(R.string.invite_prepare_error))
                } else {
                    Log.w(TAG, "Invitación omitida: ${result.errorMessage}")
                }
                return false
            }
            if (fromQueue) {
                sendLauncher.launch(result.intent)
            } else {
                startActivity(result.intent)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo correo", e)
            if (!fromQueue) {
                showMessage(getString(R.string.invite_email_error))
            }
            false
        }
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private inner class InviteAdapter(
        private val items: List<Invitado>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Invitado = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@InvitesActivity)
                .inflate(R.layout.item_invite, parent, false)

            val item = items[position]
            view.findViewById<TextView>(R.id.nombreText).text = item.nombreCompleto
            view.findViewById<TextView>(R.id.pmiText).text = "PMI ID: ${item.pmiId}"
            view.findViewById<TextView>(R.id.emailText).text = item.email
            view.findViewById<View>(R.id.sendButton).isEnabled = !sendingBulk
            view.findViewById<View>(R.id.sendButton).setOnClickListener {
                sendInvitation(item)
            }
            return view
        }
    }

    companion object {
        private const val TAG = "InvitesActivity"
    }
}
