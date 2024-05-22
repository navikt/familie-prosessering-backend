package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLoggRepository
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.transaction.TestTransaction

class TaskWorkerTest : IntegrationRunnerTest() {
    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var repository: TaskRepository

    @Autowired
    private lateinit var taskLoggRepository: TaskLoggRepository

    @Autowired
    private lateinit var worker: TaskWorker

    @Test
    fun `skal behandle task`() {
        var task = taskService.save(Task(TaskStep1.TASK_1, "{'a'='b'}"))
        task = taskService.plukker(task)
        assertThat(task.status).isEqualTo(Status.PLUKKET)
        TestTransaction.flagForCommit()
        TestTransaction.end()
        worker.doActualWork(task.id)

        val findByIdOrNull = repository.findByIdOrNull(task.id)
        val taskLogg = taskLoggRepository.findByTaskId(task.id).sortedBy { it.opprettetTid }

        assertThat(findByIdOrNull?.status).isEqualTo(Status.FERDIG)
        assertThat(taskLogg).hasSize(4)
    }
}
