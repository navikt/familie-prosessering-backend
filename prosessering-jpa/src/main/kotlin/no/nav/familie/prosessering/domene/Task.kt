package no.nav.familie.prosessering.domene

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import org.slf4j.MDC
import java.io.IOException
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "TASK")
data class Task(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
        @SequenceGenerator(name = "task_seq")
        override val id: Long = 0L,
        override val payload: String,
        @Enumerated(EnumType.STRING)
        override val status: Status = Status.UBEHANDLET,
        @Enumerated(EnumType.STRING)
        override val avvikstype: Avvikstype? = null,
        override val opprettetTid: LocalDateTime = LocalDateTime.now(),
        override val triggerTid: LocalDateTime = LocalDateTime.now(),
        override val type: String,
        override val metadata: String = "",
        @Version
        override val versjon: Long = 0,
        // Setter fetch til eager fordi AsyncTask ikke får lastet disse hvis ikke den er prelastet.
        @OneToMany(fetch = FetchType.EAGER,
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH],
                   orphanRemoval = true)
        @JoinColumn(name = "task_id")
        override val logg: MutableList<TaskLogg> = mutableListOf(TaskLogg(type = Loggtype.UBEHANDLET))

) : ITask() {

    constructor(type: String, payload: String) :
            this(type, payload, Properties())

    private constructor (type: String, payload: String, properties: Properties) :
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
                    logg = (logg + TaskLogg(type = Loggtype.AVVIKSHÅNDTERT,
                                           melding = årsak,
                                           endretAv = endretAv)).toMutableList())
    }

    override fun behandler(): Task {
        return copy(status = Status.BEHANDLER, logg = (logg + TaskLogg(type = Loggtype.BEHANDLER)).toMutableList())
    }

    override fun klarTilPlukk(endretAv: String): Task {
        return copy(status = Status.KLAR_TIL_PLUKK,
                    logg = (logg + TaskLogg(type = Loggtype.KLAR_TIL_PLUKK, endretAv = endretAv)).toMutableList())
    }

    override fun plukker(): Task {
        return copy(status = Status.PLUKKET, logg = (logg + TaskLogg(type = Loggtype.PLUKKET)).toMutableList())
    }

    override fun ferdigstill(): Task {
        return copy(status = Status.FERDIG, logg = (logg + TaskLogg(type = Loggtype.FERDIG)).toMutableList())
    }

    override fun feilet(feil: TaskFeil, maxAntallFeil: Int, settTilManuellBehandling: Boolean): Task {
        if (this.status == Status.MANUELL_OPPFØLGING) {
            return this.copy(logg = (logg + TaskLogg(type = Loggtype.MANUELL_OPPFØLGING,
                                                    melding = feil.writeValueAsString())).toMutableList())
        }

        val nyStatus = nyFeiletStatus(maxAntallFeil, settTilManuellBehandling)

        return try {
            this.copy(status = nyStatus,
                      logg = (logg + TaskLogg(type = Loggtype.FEILET, melding = feil.writeValueAsString())).toMutableList())

        } catch (e: IOException) {
            this.copy(status = nyStatus, logg = (logg + TaskLogg(type = Loggtype.FEILET)).toMutableList())
        }
    }

    override fun medTriggerTid(plusSeconds: LocalDateTime): Task {
        return this.copy(triggerTid = plusSeconds)
    }

}
