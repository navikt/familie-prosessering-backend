package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
class ScheduledTasksServiceTest {


    @Autowired
    private lateinit var scheduledTasksService: ScheduledTaskService

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @AfterEach
    fun clear() {
        taskRepository.deleteAll()
    }

    @Test
    @Sql("classpath:sql-testdata/gamle_tasker_med_logg.sql")
    @DirtiesContext
    fun `skal slette gamle tasker med status FERDIG`() {
        scheduledTasksService.slettTasksKlarForSletting()

        assertThat(taskRepository.findAll())
                .hasSize(1)
                .extracting("status").containsOnly(Status.KLAR_TIL_PLUKK)
    }

    @Test
    @DirtiesContext
    fun `skal ikke slette nye tasker`() {
        val nyTask = Task("type", "payload").ferdigstill()
        val saved = taskRepository.save(nyTask)


        scheduledTasksService.slettTasksKlarForSletting()

        assertThat(taskRepository.findAll())
                .filteredOn("id", saved.id)
                .isNotEmpty
    }

    @Test
    @DirtiesContext
    fun `skal sette feilede tasks klar til plukk`() {
        var task = Task("type", "payload")
        task = task.feilet(TaskFeil(task, null), 0, false)
        val saved = taskRepository.save(task)


        scheduledTasksService.retryFeilendeTask()

        assertThat(taskRepository.findByIdOrNull(saved.id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }


}
