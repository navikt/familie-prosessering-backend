package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLoggRepository
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.transaction.TestTransaction
import java.time.LocalDateTime

class TaskSchedulerTest : IntegrationRunnerTest() {

    @Autowired
    private lateinit var tasksScheduler: TaskScheduler

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var taskLoggRepository: TaskLoggRepository

    @Test
    @Sql("classpath:sql-testdata/gamle_tasker_med_logg.sql")
    fun `skal slette gamle tasker med status FERDIG`() {
        TestTransaction.flagForCommit()
        TestTransaction.end()
        tasksScheduler.slettTasksKlarForSletting()

        assertThat(taskRepository.findAll())
            .hasSize(1)
            .extracting("status").containsOnly(Status.KLAR_TIL_PLUKK)
        assertThat(taskLoggRepository.findAll().map { it.taskId }).containsOnly(1000002)
    }

    @Test
    fun `skal ikke slette nye tasker`() {
        val nyTask = taskService.save(Task("type", "payload"))
        val saved = taskService.ferdigstill(nyTask)

        tasksScheduler.slettTasksKlarForSletting()

        assertThat(taskRepository.findAll())
            .filteredOn("id", saved.id)
            .isNotEmpty
    }

    @Test
    fun `skal sette feilede tasks klar til plukk`() {
        var task = taskService.save(Task("type", "payload"))
        task = taskService.feilet(task, TaskFeil(task, null), 0, 0, false)
        val saved = taskService.save(task)

        tasksScheduler.retryFeilendeTask()

        assertThat(taskRepository.findByIdOrNull(saved.id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }

    @Test
    fun `skal sette tasker som har vært plukket i mer enn en time klar til plukk`() {
        var task = taskService.save(Task("type", "payload"))
        task = taskService.plukker(task)
        taskLoggRepository.findAll().forEach {
            taskLoggRepository.save(it.copy(opprettetTid = LocalDateTime.now().minusDays(2)))
        }

        tasksScheduler.settPermanentPlukketTilKlarTilPlukk()

        assertThat(taskRepository.findByIdOrNull(task.id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }

    @Test
    fun `skal ikke gjøre noe med tasker som har vært plukket i mindre enn en time`() {
        var task = taskService.save(Task("type", "payload"))
        task = taskService.plukker(task)
        taskLoggRepository.findAll().forEach {
            taskLoggRepository.save(it.copy(opprettetTid = LocalDateTime.now().minusMinutes(59)))
        }

        tasksScheduler.settPermanentPlukketTilKlarTilPlukk()

        assertThat(taskRepository.findByIdOrNull(task.id)!!.status).isEqualTo(Status.PLUKKET)
    }
}
