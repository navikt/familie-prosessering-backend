package no.nav.familie.prosessering.internal

import io.mockk.mockk
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TaskWorkerDuplikatTest {
    @Test
    fun `skal ikke kunne ha 2 tasker med samme taskStepType`() {
        assertThatThrownBy {
            TaskWorker(mockk(), listOf(Task1(), Task1()))
        }.hasMessage("Flere tasker har samme taskStepType(task1)")
    }

    @Test
    fun `skal kunne initiere taskWorker med steg med ulike taskStepType`() {
        TaskWorker(mockk(), listOf(Task1(), Task2()))
    }

    @TaskStepBeskrivelse(taskStepType = "task1", beskrivelse = "")
    private class Task1 : AsyncTaskStep {
        override fun doTask(task: Task) {
        }
    }

    @TaskStepBeskrivelse(taskStepType = "task2", beskrivelse = "")
    private class Task2 : AsyncTaskStep {
        override fun doTask(task: Task) {
        }
    }
}
