package no.nav.familie.prosessering.external

import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
internal class TaskServiceTest {


    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Test
    fun `skal lagre og kjøre task med en gang`() {
        taskService.saveAndRun(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))

        Assertions.assertThat(taskRepository.findByStatus(Status.FERDIG)).hasSizeGreaterThan(0)
    }
}