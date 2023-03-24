package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@TaskStepBeskrivelse(taskStepType = TaskStep1_NesteTaskMedDefaultConstructor.TASK_1, beskrivelse = "Dette er task 1")
class TaskStep1_NesteTaskMedDefaultConstructor constructor(private val taskService: TaskService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        try {
            TimeUnit.MICROSECONDS.sleep(1)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onCompletion(task: Task) {
        val nesteTask = Task(type = TaskStep2.TASK_2, payload = task.payload, status = Status.UBEHANDLET)
        taskService.save(nesteTask)
    }

    companion object {

        const val TASK_1 = "task1DefaultConstructor"
    }
}
