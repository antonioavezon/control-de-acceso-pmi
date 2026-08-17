package cl.controlacceso.domain

import cl.controlacceso.model.Invitado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRulesTest {

    @Test
    fun firstEntrySetsHora() {
        assertEquals(1000L, EventRules.resolveHoraEntrada(null, 1000L))
    }

    @Test
    fun reentryPreservesFirstHora() {
        assertEquals(1000L, EventRules.resolveHoraEntrada(1000L, 2000L))
    }

    @Test
    fun uniqueAttendeesDoNotCountScans() {
        val list = listOf(
            Invitado("1", "A", "B", "a@t.cl", 1000L, true),
            Invitado("2", "C", "D", "c@t.cl", null, false)
        )
        assertEquals(1, EventRules.countUniqueAttendees(list))
    }

    @Test
    fun deleteBlockedWithoutExport() {
        assertFalse(EventRules.canDeleteEventData(false))
    }

    @Test
    fun deleteAllowedAfterExport() {
        assertTrue(EventRules.canDeleteEventData(true))
    }

    @Test
    fun detectsReentry() {
        val first = Invitado("1", "A", "B", "a@t.cl", null, false)
        val again = Invitado("1", "A", "B", "a@t.cl", 1000L, true)
        assertFalse(EventRules.isReentry(first))
        assertTrue(EventRules.isReentry(again))
    }
}
