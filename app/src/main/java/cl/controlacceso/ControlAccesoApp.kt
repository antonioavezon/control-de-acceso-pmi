package cl.controlacceso

import android.app.Application
import cl.controlacceso.data.EmailTemplateRepository
import cl.controlacceso.data.EventRepository

class ControlAccesoApp : Application() {

    lateinit var eventRepository: EventRepository
        private set

    lateinit var emailTemplateRepository: EmailTemplateRepository
        private set

    override fun onCreate() {
        super.onCreate()
        eventRepository = EventRepository(this)
        emailTemplateRepository = EmailTemplateRepository(this)
    }
}
