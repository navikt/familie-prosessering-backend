package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Prioritet
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@TaskStepBeskrivelse(taskStepType = TaskStep1AnnenPrioritet.TASK_1, beskrivelse = "Dette er task 1")
class TaskStep1AnnenPrioritet constructor(private val taskService: TaskService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        try {
            TimeUnit.MICROSECONDS.sleep(1)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onCompletion(task: Task) {
        val nesteTask = Task(type = TaskStep2.TASK_2, payload = task.payload, prioritet = Prioritet.VIKTIG)
        taskService.save(nesteTask)
    }

    companion object {

        const val TASK_1 = "task1AnnenPrioPåNesteTask"
    }
}
