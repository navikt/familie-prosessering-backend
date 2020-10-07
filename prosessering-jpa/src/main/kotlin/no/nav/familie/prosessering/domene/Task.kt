package no.nav.familie.prosessering.domene

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.ITaskLogg.Companion.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
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
        override var status: Status = Status.UBEHANDLET,
        @Enumerated(EnumType.STRING)
        override var avvikstype: Avvikstype? = null,
        override var opprettetTid: LocalDateTime = LocalDateTime.now(),
        override var triggerTid: LocalDateTime = LocalDateTime.now(),
        override val type: String,
        override val metadata: String = "",
        @Version
        override val versjon: Long = 0,
        // Setter fetch til eager fordi AsyncTask ikke får lastet disse hvis ikke den er prelastet.
        @OneToMany(fetch = FetchType.EAGER,
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH],
                   orphanRemoval = true)
        @JoinColumn(name = "task_id")
        override val logg: MutableList<TaskLogg> = arrayListOf(TaskLogg(type = Loggtype.UBEHANDLET))

) : ITask() {

    override val callId: String
        get() = this.metadataProperties().getProperty(MDCConstants.MDC_CALL_ID)

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

        this.status = Status.AVVIKSHÅNDTERT
        this.avvikstype = avvikstype
        this.logg.add(TaskLogg(type = Loggtype.AVVIKSHÅNDTERT,
                               melding = årsak,
                               endretAv = endretAv))
        return this
    }

    override fun behandler(): Task {
        this.status = Status.BEHANDLER
        this.logg.add(TaskLogg(type = Loggtype.BEHANDLER))
        return this
    }

    override fun klarTilPlukk(endretAv: String): Task {
        this.status = Status.KLAR_TIL_PLUKK
        this.logg.add(TaskLogg(type = Loggtype.KLAR_TIL_PLUKK,
                               melding = null,
                               endretAv = endretAv))
        return this
    }

    override fun plukker(): Task {
        this.status = Status.PLUKKET
        this.logg.add(TaskLogg(type = Loggtype.PLUKKET))
        return this
    }

    override fun ferdigstill(): Task {
        this.status = Status.FERDIG
        this.logg.add(TaskLogg(type = Loggtype.FERDIG))
        return this
    }

    override fun feilet(feil: TaskFeil, maxAntallFeil: Int): Task {
        if (this.status == Status.MANUELL_OPPFØLGING) {
            logg.add(TaskLogg(type = Loggtype.MANUELL_OPPFØLGING,
                              melding = feil.writeValueAsString(),
                              endretAv = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
            return this
        }

        try {
            this.logg.add(TaskLogg(type = Loggtype.FEILET,
                                   melding = feil.writeValueAsString(),
                                   endretAv = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
        } catch (e: IOException) {
            this.logg.add(TaskLogg(type = Loggtype.FEILET))
        }

        val antallFeilendeForsøk = logg
                .filter { it.type == Loggtype.FEILET }
                .size
        if (maxAntallFeil > antallFeilendeForsøk) {
            this.status = Status.KLAR_TIL_PLUKK
        } else {
            this.status = Status.FEILET
        }
        return this
    }

    override fun copy(id: Long,
                      payload: String,
                      status: Status,
                      avvikstype: Avvikstype?,
                      opprettetTidspunkt: LocalDateTime,
                      triggerTid: LocalDateTime,
                      taskStepType: String,
                      metadataString: String,
                      versjon: Long,
                      logg: List<ITaskLogg>): ITask {

        return Task(id,
                    payload,
                    status,
                    avvikstype,
                    opprettetTidspunkt,
                    triggerTid,
                    taskStepType,
                    metadataString,
                    versjon,
                    logg.map { it as TaskLogg }.toMutableList())
    }

}
