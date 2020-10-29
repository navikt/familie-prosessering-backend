package no.nav.familie.prosessering.domene

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TaskFeil
import java.time.LocalDateTime
import java.util.*


abstract class ITask {

    abstract val id: Long

    abstract val payload: String

    abstract val status: Status

    abstract val avvikstype: Avvikstype?

    abstract val opprettetTid: LocalDateTime

    abstract val triggerTid: LocalDateTime

    abstract val type: String

    abstract val metadata: Properties

    abstract val versjon: Long

    abstract val logg: Collection<ITaskLogg>

    val callId: String
        get() = metadata.getProperty(MDCConstants.MDC_CALL_ID)

    abstract fun avvikshåndter(avvikstype: Avvikstype,
                               årsak: String,
                               endretAv: String): ITask

    abstract fun behandler(): ITask
    abstract fun klarTilPlukk(endretAv: String): ITask
    abstract fun plukker(): ITask
    abstract fun ferdigstill(): ITask
    abstract fun feilet(feil: TaskFeil, maxAntallFeil: Int, settTilManuellOppfølgning: Boolean): ITask
    abstract fun medTriggerTid(plusSeconds: LocalDateTime): ITask

    protected fun nyFeiletStatus(maxAntallFeil: Int, settTilManuellOppfølgning: Boolean): Status {
        val antallFeilendeForsøk = logg.count { it.type == Loggtype.FEILET } + 1
        return when {
            maxAntallFeil > antallFeilendeForsøk -> Status.KLAR_TIL_PLUKK
            settTilManuellOppfølgning -> Status.MANUELL_OPPFØLGING
            else -> Status.FEILET
        }
    }

}