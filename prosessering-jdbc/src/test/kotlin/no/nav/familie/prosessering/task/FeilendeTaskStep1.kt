package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = FeilendeTaskStep1.FEILENDE_TASK_1, beskrivelse = "Dette er feilende task 1")
class FeilendeTaskStep1 @Autowired constructor(private val taskRepository: TaskRepository) : AsyncTaskStep {


    override fun doTask(task: ITask) {
        throw RuntimeException("feiler")
    }

    override fun onCompletion(task: ITask) {
        taskRepository.save(Task(TaskStep2.TASK_2, task.payload))
    }

    companion object {

        const val FEILENDE_TASK_1 = "feilendeTask1"
    }

}
