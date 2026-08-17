package cl.controlacceso.domain

import cl.controlacceso.model.Invitado

/**
 * Reglas de negocio puras (sin Android) para facilitar pruebas unitarias.
 */
object EventRules {

    fun canDeleteEventData(resumenExportado: Boolean): Boolean = resumenExportado

    fun countUniqueAttendees(participantes: List<Invitado>): Int =
        participantes.count { it.ingresoRegistrado && it.horaEntrada != null }

    /**
     * La hora del primer ingreso es inmutable.
     */
    fun resolveHoraEntrada(existing: Long?, newTimestamp: Long): Long =
        existing ?: newTimestamp

    fun isReentry(participante: Invitado): Boolean =
        participante.ingresoRegistrado && participante.horaEntrada != null
}
