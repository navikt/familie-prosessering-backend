package no.nav.familie.prosessering.domene

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import org.slf4j.MDC
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

@Table("task")
data class Task(
        @Id
        override val id: Long = 0L,
        override val payload: String,
        override val status: Status = Status.UBEHANDLET,
        override val avvikstype: Avvikstype? = null,
        override val opprettetTid: LocalDateTime = LocalDateTime.now(),
        override val triggerTid: LocalDateTime = LocalDateTime.now(),
        override val type: String,
        @Column("metadata")
        val metadataWrapper: PropertiesWrapper = PropertiesWrapper(Properties().apply {
            this[MDCConstants.MDC_CALL_ID] =
                    MDC.get(MDCConstants.MDC_CALL_ID)
                    ?: IdUtils.generateId()
        }),
        @Version
        override val versjon: Long = 0,
        @MappedCollection(idColumn = "task_id")
        override val logg: Set<TaskLogg> = setOf(TaskLogg(type = Loggtype.UBEHANDLET))
) : ITask() {



    @Transient
    override val metadata: Properties = metadataWrapper.properties

    constructor (type: String, payload: String, properties: Properties = Properties()) :
            this(type = type,
                 payload = payload,
                 metadataWrapper = PropertiesWrapper(properties.apply {
                     this[MDCConstants.MDC_CALL_ID] =
                             MDC.get(MDCConstants.MDC_CALL_ID)
                             ?: IdUtils.generateId()
                 }))

    override fun avvikshåndter(avvikstype: Avvikstype,
                               årsak: String,
                               endretAv: String): Task {

        return copy(status = Status.AVVIKSHÅNDTERT,
                    avvikstype = avvikstype,
                    logg = logg + TaskLogg(type = Loggtype.AVVIKSHÅNDTERT,
                                           melding = årsak,
                                           endretAv = endretAv))
    }

    override fun kommenter(kommentar: String, endretAv: String): Task {

        return copy(logg = logg + TaskLogg(type = Loggtype.KOMMENTAR,
                                           melding = kommentar,
                                           endretAv = endretAv))
    }

    override fun behandler(): Task {
        return copy(status = Status.BEHANDLER, logg = logg + TaskLogg(type = Loggtype.BEHANDLER))
    }

    override fun klarTilPlukk(endretAv: String, melding: String?): Task {
        return copy(status = Status.KLAR_TIL_PLUKK,
                    logg = logg + TaskLogg(type = Loggtype.KLAR_TIL_PLUKK, endretAv = endretAv, melding = melding))
    }

    override fun plukker(): Task {
        return copy(status = Status.PLUKKET, logg = logg + TaskLogg(type = Loggtype.PLUKKET))
    }

    override fun ferdigstill(): Task {
        return copy(status = Status.FERDIG, logg = logg + TaskLogg(type = Loggtype.FERDIG))
    }

    override fun feilet(feil: TaskFeil, maxAntallFeil: Int, settTilManuellOppfølgning: Boolean): Task {
        if (this.status == Status.MANUELL_OPPFØLGING) {
            return this.copy(logg = logg + TaskLogg(type = Loggtype.MANUELL_OPPFØLGING,
                                                    melding = feil.writeValueAsString()))
        }

        val nyStatus = nyFeiletStatus(maxAntallFeil, settTilManuellOppfølgning)

        return try {
            this.copy(status = nyStatus,
                      logg = logg + TaskLogg(type = Loggtype.FEILET, melding = feil.writeValueAsString()))

        } catch (e: IOException) {
            this.copy(status = nyStatus, logg = logg + TaskLogg(type = Loggtype.FEILET))
        }
    }

    override fun medTriggerTid(triggerTid: LocalDateTime): Task {
        return this.copy(triggerTid = triggerTid)
    }

    override fun toString(): String {
        return "Task(id=$id, status=$status, opprettetTid=$opprettetTid, triggerTid=$triggerTid, type='$type', versjon=$versjon)"
    }

}
