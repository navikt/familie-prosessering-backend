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
    val id: Long = 0L,
    val payload: String,
    val status: Status = Status.UBEHANDLET,
    val avvikstype: Avvikstype? = null,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    val triggerTid: LocalDateTime = LocalDateTime.now(),
    val type: String,
    @Column("metadata")
    val metadataWrapper: PropertiesWrapper = PropertiesWrapper(
        Properties().apply {
            this[MDCConstants.MDC_CALL_ID] =
                MDC.get(MDCConstants.MDC_CALL_ID)
                    ?: IdUtils.generateId()
        }
    ),
    @Version
    val versjon: Long = 0,
    @MappedCollection(idColumn = "task_id")
    val logg: Set<TaskLogg> = setOf(TaskLogg(type = Loggtype.UBEHANDLET, node = hentNodeNavn()))
) {

    @Transient
    val metadata: Properties = metadataWrapper.properties

    val callId: String
        get() = metadata.getProperty(MDCConstants.MDC_CALL_ID)

    constructor (type: String, payload: String, properties: Properties = Properties()) :
        this(
            type = type,
            payload = payload,
            metadataWrapper = PropertiesWrapper(
                properties.apply {
                    this[MDCConstants.MDC_CALL_ID] =
                        MDC.get(MDCConstants.MDC_CALL_ID)
                            ?: IdUtils.generateId()
                }
            )
        )

    fun avvikshåndter(
        avvikstype: Avvikstype,
        årsak: String,
        endretAv: String
    ): Task {

        return copy(
            status = Status.AVVIKSHÅNDTERT,
            avvikstype = avvikstype,
            logg = logg + TaskLogg(
                type = Loggtype.AVVIKSHÅNDTERT,
                melding = årsak,
                endretAv = endretAv,
                node = hentNodeNavn()
            )
        )
    }

    fun kommenter(kommentar: String, endretAv: String, settTilManuellOppfølgning: Boolean): Task {

        if (settTilManuellOppfølgning) {
            return this.copy(
                status = Status.MANUELL_OPPFØLGING,
                logg = logg + TaskLogg(
                    type = Loggtype.KOMMENTAR,
                    melding = kommentar,
                    endretAv = endretAv,
                    node = hentNodeNavn()
                )
            )
        } else {
            return copy(
                logg = logg + TaskLogg(
                    type = Loggtype.KOMMENTAR,
                    melding = kommentar,
                    endretAv = endretAv,
                    node = hentNodeNavn()
                )
            )
        }
    }

    fun behandler(): Task {
        return copy(status = Status.BEHANDLER, logg = logg + TaskLogg(type = Loggtype.BEHANDLER, node = hentNodeNavn()))
    }

    fun klarTilPlukk(endretAv: String, melding: String? = null): Task {
        return copy(
            status = Status.KLAR_TIL_PLUKK,
            logg = logg + TaskLogg(type = Loggtype.KLAR_TIL_PLUKK, endretAv = endretAv, melding = melding, node = hentNodeNavn())
        )
    }

    fun plukker(): Task {
        return copy(status = Status.PLUKKET, logg = logg + TaskLogg(type = Loggtype.PLUKKET, node = hentNodeNavn()))
    }

    fun ferdigstill(): Task {
        return copy(status = Status.FERDIG, logg = logg + TaskLogg(type = Loggtype.FERDIG, node = hentNodeNavn()))
    }

    fun feilet(feil: TaskFeil, maxAntallFeil: Int, settTilManuellOppfølgning: Boolean): Task {
        if (this.status == Status.MANUELL_OPPFØLGING) {
            return this.copy(
                logg = logg + TaskLogg(
                    type = Loggtype.MANUELL_OPPFØLGING,
                    melding = feil.writeValueAsString(),
                    node = System.getenv("HOSTNAME")
                )
            )
        }

        val nyStatus = nyFeiletStatus(maxAntallFeil, settTilManuellOppfølgning)

        return try {
            this.copy(
                status = nyStatus,
                logg = logg + TaskLogg(type = Loggtype.FEILET, node = hentNodeNavn(), melding = feil.writeValueAsString())
            )
        } catch (e: IOException) {
            this.copy(status = nyStatus, logg = logg + TaskLogg(type = Loggtype.FEILET, node = hentNodeNavn()))
        }
    }

    fun medTriggerTid(triggerTid: LocalDateTime): Task {
        return this.copy(triggerTid = triggerTid)
    }

    protected fun nyFeiletStatus(maxAntallFeil: Int, settTilManuellOppfølgning: Boolean): Status {
        val antallFeilendeForsøk = logg.count { it.type == Loggtype.FEILET } + 1
        return when {
            maxAntallFeil > antallFeilendeForsøk -> Status.KLAR_TIL_PLUKK
            settTilManuellOppfølgning -> Status.MANUELL_OPPFØLGING
            else -> Status.FEILET
        }
    }

    override fun toString(): String {
        return "Task(id=$id, status=$status, opprettetTid=$opprettetTid, triggerTid=$triggerTid, type='$type', versjon=$versjon)"
    }
}

fun hentNodeNavn(): String {
    return System.getenv("HOSTNAME") ?: "node1"
}
