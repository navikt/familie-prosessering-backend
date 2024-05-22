package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = TaskStepMedError.TYPE,
    beskrivelse = "Task med error",
)
class TaskStepMedError : AsyncTaskStep {
    override fun doTask(task: Task) {
        throw NotImplementedError("Ikke implementert")
    }

    companion object {
        const val TYPE = "taskMedError"
    }
}
