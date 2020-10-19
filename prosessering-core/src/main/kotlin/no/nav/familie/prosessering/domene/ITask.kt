package no.nav.familie.prosessering.domene

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import java.time.LocalDateTime


abstract class ITask {

    abstract val id: Long

    abstract val payload: String

    abstract val status: Status

    abstract val avvikstype: Avvikstype?

    abstract val opprettetTid: LocalDateTime

    abstract val triggerTid: LocalDateTime

    abstract val type: String

    abstract val metadata: String

    abstract val versjon: Long

    abstract val logg: List<ITaskLogg>

    val callId: String
        get() = this.metadataProperties().getProperty(MDCConstants.MDC_CALL_ID)

    abstract fun avvikshåndter(avvikstype: Avvikstype,
                               årsak: String,
                               endretAv: String): ITask

    abstract fun behandler(): ITask
    abstract fun klarTilPlukk(endretAv: String): ITask
    abstract fun plukker(): ITask
    abstract fun ferdigstill(): ITask
    abstract fun feilet(feil: TaskFeil, maxAntallFeil: Int, settTilManuellBehandling: Boolean): ITask
    abstract fun medTriggerTid(plusSeconds: LocalDateTime): ITask

    protected fun nyFeiletStatus(maxAntallFeil: Int, settTilManuellBehandling: Boolean): Status {
        val antallFeilendeForsøk = logg.count { it.type == Loggtype.FEILET } + 1
        return when {
            maxAntallFeil > antallFeilendeForsøk -> Status.KLAR_TIL_PLUKK
            settTilManuellBehandling -> Status.MANUELL_OPPFØLGING
            else -> Status.FEILET
        }
    }

    fun metadataProperties() = metadata.asProperties()
}