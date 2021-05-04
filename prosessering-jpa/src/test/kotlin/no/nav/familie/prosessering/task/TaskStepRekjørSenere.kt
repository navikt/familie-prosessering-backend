package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Service
@TaskStepBeskrivelse(taskStepType = TaskStepRekjørSenere.TYPE, beskrivelse = "")
class TaskStepRekjørSenere : AsyncTaskStep {

    override fun doTask(task: Task) {
        throw RekjørSenereException("årsak", LocalDate.of(2088,1,1).atStartOfDay())
    }

    override fun onCompletion(task: Task) {}

    companion object {

        const val TYPE = "TaskStepRekjørSenere"
    }
}
