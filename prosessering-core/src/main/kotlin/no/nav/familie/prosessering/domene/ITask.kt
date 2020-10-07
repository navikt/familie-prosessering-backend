package no.nav.familie.prosessering.domene

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

    abstract val callId: String

    abstract fun avvikshåndter(avvikstype: Avvikstype,
                               årsak: String,
                               endretAv: String): ITask

    abstract fun behandler(): ITask
    abstract fun klarTilPlukk(endretAv: String): ITask
    abstract fun plukker(): ITask
    abstract fun ferdigstill(): ITask
    abstract fun feilet(feil: TaskFeil, maxAntallFeil: Int): ITask

    fun  metadataProperties() = metadata.asProperties()

    override fun toString(): String {
        return """Task(id=$id, 
            |payload='$payload', 
            |status=$status, 
            |avvikstype=$avvikstype, 
            |opprettetTidspunkt=$opprettetTid, 
            |triggerTid=$triggerTid, 
            |type='$type', 
            |metadata=$metadata)""".trimMargin()
    }

    abstract fun copy(id: Long = this.id,
                      payload: String = this.payload,
                      status: Status = this.status,
                      avvikstype: Avvikstype? = this.avvikstype,
                      opprettetTidspunkt: LocalDateTime = this.opprettetTid,
                      triggerTid: LocalDateTime = this.triggerTid,
                      taskStepType: String = this.type,
                      metadataString: String = this.metadata,
                      versjon: Long = this.versjon,
                      logg: List<ITaskLogg> = this.logg): ITask

}