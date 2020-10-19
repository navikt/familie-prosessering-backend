package no.nav.familie.prosessering.domene

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import org.slf4j.MDC
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

@Table("TASK")
data class Task(
        @Id
        override val id: Long = 0L,
        override val payload: String,
        override val status: Status = Status.UBEHANDLET,
        override val avvikstype: Avvikstype? = null,
        override val opprettetTid: LocalDateTime = LocalDateTime.now(),
        override val triggerTid: LocalDateTime = LocalDateTime.now(),
        override val type: String,
        override val metadata: String = "",
        @Version
        override val versjon: Long = 0,
        @MappedCollection(idColumn = "TASK_ID", keyColumn = "ID")
        override val logg: List<TaskLogg> = arrayListOf(TaskLogg(type = Loggtype.UBEHANDLET))
) : ITask() {


    constructor (type: String, payload: String, properties: Properties = Properties()) :
            this(type = type,
                 payload = payload,
                 metadata = properties.apply {
                     this[MDCConstants.MDC_CALL_ID] =
                             MDC.get(MDCConstants.MDC_CALL_ID)
                             ?: IdUtils.generateId()
                 }.asString())

    override fun avvikshåndter(avvikstype: Avvikstype,
                               årsak: String,
                               endretAv: String): Task {

        return copy(status = Status.AVVIKSHÅNDTERT,
                    avvikstype = avvikstype,
                    logg = logg + TaskLogg(type = Loggtype.AVVIKSHÅNDTERT,
                                           melding = årsak,
                                           endretAv = endretAv))
    }

    override fun behandler(): Task {
        return copy(status = Status.BEHANDLER, logg = logg + TaskLogg(type = Loggtype.BEHANDLER))
    }

    override fun klarTilPlukk(endretAv: String): Task {
        return copy(status = Status.KLAR_TIL_PLUKK,
                    logg = logg + TaskLogg(type = Loggtype.KLAR_TIL_PLUKK, endretAv = endretAv))
    }

    override fun plukker(): Task {
        return copy(status = Status.PLUKKET, logg = logg + TaskLogg(type = Loggtype.PLUKKET))
    }

    override fun ferdigstill(): Task {
        return copy(status = Status.FERDIG, logg = logg + TaskLogg(type = Loggtype.FERDIG))
    }

    override fun feilet(feil: TaskFeil, maxAntallFeil: Int): Task {
        if (this.status == Status.MANUELL_OPPFØLGING) {
            return this.copy(logg = logg + TaskLogg(type = Loggtype.MANUELL_OPPFØLGING,
                                                    melding = feil.writeValueAsString()))
        }

        val antallFeilendeForsøk = logg
                                           .filter { it.type == Loggtype.FEILET }
                                           .size + 1
        val nyStatus = if (maxAntallFeil > antallFeilendeForsøk) Status.KLAR_TIL_PLUKK else Status.FEILET

        return try {
            this.copy(status = nyStatus,
                      logg = logg + TaskLogg(type = Loggtype.FEILET, melding = feil.writeValueAsString()))

        } catch (e: IOException) {
            this.copy(status = nyStatus, logg = logg + TaskLogg(type = Loggtype.FEILET))
        }
    }

    override fun medTriggerTid(plusSeconds: LocalDateTime): Task {
        return this.copy(triggerTid = plusSeconds)
    }

}
