package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class TaskRepositoryTest {

    @Autowired
    private lateinit var repository: TaskRepository


    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker uavhengig av status`() {
        val preCount = repository.findByStatusIn(Status.values().toList(), PageRequest.of(0, 1000))
        val preCountFeilet = preCount.count { it.status == Status.FEILET }
        val preCountUbehandlet = preCount.count { it.status == Status.UBEHANDLET }

        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.FEILET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.UBEHANDLET)

        repository.save(ubehandletTask)
        repository.save(feiletTask1)
        repository.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(Status.values().toList(), PageRequest.of(0, 1000))
        Assertions.assertThat(alleTasks.size).isEqualTo(3 + preCount.size)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2 + preCountFeilet)
        Assertions.assertThat(alleTasks.count { it.status == Status.UBEHANDLET }).isEqualTo(1 + preCountUbehandlet)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker gitt en status`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status =Status.UBEHANDLET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status =Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status =Status.FEILET)

        repository.save(ubehandletTask)
        repository.save(feiletTask1)
        repository.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(listOf(Status.FEILET), PageRequest.of(0, 1000))
        Assertions.assertThat(alleTasks.size).isEqualTo(2)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut max 1 task gitt en status`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)

        repository.save(ubehandletTask)
        repository.save(feiletTask1)
        repository.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(listOf(Status.FEILET), PageRequest.of(0, 1))
        Assertions.assertThat(alleTasks.size).isEqualTo(1)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(1)
    }

    @Test
    fun `skal håndtere properties`() {
        val property = "PROPERTY"
        val lagretTask = repository.save(Task(type = TaskStep1.TASK_1,
                                              payload = "{'a'='b'}",
                                              properties = Properties().apply {
                                                  this[property] = property
                                              }))
        val task = repository.findByIdOrNull(lagretTask.id)!!
        Assertions.assertThat(task.metadata.getProperty(property)).isEqualTo(property)
    }

}
