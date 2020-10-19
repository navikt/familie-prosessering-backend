package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TaskStepFeilManuellBehandling.TASK_FEIL_1,
                     beskrivelse = "Dette er task 1",
                     settTilManuellOppfølgning = true)
class TaskStepFeilManuellBehandling : AsyncTaskStep {


    override fun doTask(task: ITask) {
        error("Feiler")
    }

    companion object {

        const val TASK_FEIL_1 = "taskFeilManuellBehandling1"
    }

}
