package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Service
@TaskStepBeskrivelse(taskStepType = TaskStepExceptionUtenStackTrace.TYPE, beskrivelse = "")
class TaskStepExceptionUtenStackTrace : AsyncTaskStep {

    override fun doTask(task: Task) {
        throw TaskExceptionUtenStackTrace("feilmelding")
    }

    override fun onCompletion(task: Task) {}

    companion object {

        const val TYPE = "TaskStepExceptionUtenStackTrace"
    }
}
