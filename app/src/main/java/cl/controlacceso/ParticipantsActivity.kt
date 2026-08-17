package cl.controlacceso

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cl.controlacceso.databinding.ActivityParticipantsBinding
import cl.controlacceso.model.Invitado

class ParticipantsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParticipantsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cl.controlacceso.util.UiHelpers.liftContentAboveNavBar(binding.root)

        binding.backButton.setOnClickListener { finish() }
        displayList()
    }

    private fun displayList() {
        val participantes = try {
            (application as ControlAccesoApp).eventRepository.getAllInvitados()
        } catch (e: Exception) {
            emptyList()
        }

        if (participantes.isEmpty()) {
            binding.emptyParticipantsText.visibility = View.VISIBLE
            binding.participantsList.visibility = View.GONE
            return
        }

        binding.emptyParticipantsText.visibility = View.GONE
        binding.participantsList.visibility = View.VISIBLE
        binding.participantsList.adapter = ParticipantAdapter(participantes)
    }

    private inner class ParticipantAdapter(
        private val items: List<Invitado>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Invitado = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ParticipantsActivity)
                .inflate(R.layout.item_participant, parent, false)
            val item = items[position]
            view.findViewById<TextView>(R.id.nombreText).text = item.nombreCompleto
            view.findViewById<TextView>(R.id.pmiText).text = "PMI ID: ${item.pmiId}"
            view.findViewById<TextView>(R.id.emailText).text = item.email
            return view
        }
    }
}
