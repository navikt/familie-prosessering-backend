package no.nav.familie.prosessering.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.ITask
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@TaskStepBeskrivelse(taskStepType = TaskStep2.TASK_2, beskrivelse = "Dette er task 2")
class TaskStep2 : AsyncTaskStep {

    override fun doTask(task: ITask) {
        try {
            TimeUnit.MICROSECONDS.sleep(1)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onCompletion(task: ITask) {}

    companion object {
        const val TASK_2 = "task2"
    }
}
