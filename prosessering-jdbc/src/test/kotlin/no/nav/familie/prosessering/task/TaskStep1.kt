package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.jdbc.Task
import no.nav.familie.prosessering.domene.jdbc.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@TaskStepBeskrivelse(taskStepType = TaskStep1.TASK_1, beskrivelse = "Dette er task 1")
class TaskStep1 @Autowired constructor(private val taskRepository: TaskRepository) : AsyncTaskStep {


    override fun doTask(task: ITask) {
        try {
            TimeUnit.MICROSECONDS.sleep(1)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onCompletion(task: ITask) {
        val nesteTask =
                Task(TaskStep2.TASK_2, task.payload)
        taskRepository.save(nesteTask)
    }

    companion object {
        const val TASK_1 = "task1"
    }

}
