package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = TaskStepMedFeil.TYPE,
    beskrivelse = "Task med feil",
)
class TaskStepMedFeil : AsyncTaskStep {
    override fun doTask(task: Task) {
        error("Feil")
    }

    companion object {
        const val TYPE = "taskMedFeil"
    }
}
