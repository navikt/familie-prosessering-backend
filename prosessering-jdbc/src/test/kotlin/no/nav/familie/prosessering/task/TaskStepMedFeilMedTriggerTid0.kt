package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TaskStepMedFeilMedTriggerTid0.TYPE,
                     beskrivelse = "Task med feil",
                     triggerTidVedFeilISekunder = 0)
class TaskStepMedFeilMedTriggerTid0 : AsyncTaskStep {

    override fun doTask(task: Task) {
        error("Feil")
    }

    companion object {

        const val TYPE = "taskMedFeilMedTriggerTid0"
    }
}
