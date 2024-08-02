package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TaskStepExceptionUtenStackTrace.TYPE, beskrivelse = "")
class TaskStepExceptionUtenStackTrace : AsyncTaskStep {
    override fun doTask(task: Task): Unit = throw TaskExceptionUtenStackTrace("feilmelding")

    override fun onCompletion(task: Task) {}

    companion object {
        const val TYPE = "TaskStepExceptionUtenStackTrace"
    }
}
