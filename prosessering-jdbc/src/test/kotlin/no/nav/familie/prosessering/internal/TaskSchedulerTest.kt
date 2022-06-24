package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDateTime

class TaskSchedulerTest : IntegrationRunnerTest() {

    @Autowired
    private lateinit var tasksScheduler: TaskScheduler

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Test
    @Sql("classpath:sql-testdata/gamle_tasker_med_logg.sql")
    fun `skal slette gamle tasker med status FERDIG`() {
        tasksScheduler.slettTasksKlarForSletting()

        assertThat(taskRepository.findAll())
            .hasSize(1)
            .extracting("status").containsOnly(Status.KLAR_TIL_PLUKK)
    }

    @Test
    fun `skal ikke slette nye tasker`() {
        val nyTask = Task("type", "payload").ferdigstill()
        val saved = taskRepository.save(nyTask)

        tasksScheduler.slettTasksKlarForSletting()

        assertThat(taskRepository.findAll())
            .filteredOn("id", saved.id)
            .isNotEmpty
    }

    @Test
    fun `skal sette feilede tasks klar til plukk`() {
        var task = Task("type", "payload")
        task = task.feilet(TaskFeil(task, null), 0, false)
        val saved = taskRepository.save(task)

        tasksScheduler.retryFeilendeTask()

        assertThat(taskRepository.findByIdOrNull(saved.id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }

    @Test
    fun `skal sette tasker som har vært plukket i mer enn en time klar til plukk`() {
        var task = Task("type", "payload").plukker()
        task = task.copy(logg = setOf(task.logg.last().copy(opprettetTid = LocalDateTime.now().minusMinutes(61))))
        val saved = taskRepository.save(task)

        tasksScheduler.settPermanentPlukketTilKlarTilPlukk()

        assertThat(taskRepository.findByIdOrNull(saved.id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }

    @Test
    fun `skal ikke gjøre noe med tasker som har vært plukket i mindre enn en time`() {
        var task = Task("type", "payload").plukker()
        task = task.copy(logg = setOf(task.logg.last().copy(opprettetTid = LocalDateTime.now().minusMinutes(59))))
        val saved = taskRepository.save(task)

        tasksScheduler.settPermanentPlukketTilKlarTilPlukk()

        assertThat(taskRepository.findByIdOrNull(saved.id)!!.status).isEqualTo(Status.PLUKKET)
    }
}
