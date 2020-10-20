package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TaskStepFeilManuellOppfølgning.TASK_FEIL_1,
                     beskrivelse = "Dette er task 1",
                     settTilManuellOppfølgning = true)
class TaskStepFeilManuellOppfølgning : AsyncTaskStep {


    override fun doTask(task: Task) {
        error("Feiler")
    }

    companion object {

        const val TASK_FEIL_1 = "taskFeilManuellOppfølgning1"
    }

}
