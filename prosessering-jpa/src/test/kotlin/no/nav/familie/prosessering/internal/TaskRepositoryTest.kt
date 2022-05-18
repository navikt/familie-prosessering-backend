package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import no.nav.familie.prosessering.util.isOptimisticLocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.transaction.TestTransaction
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class TaskRepositoryTest {

    @Autowired
    private lateinit var repository: TaskRepository

    @AfterEach
    fun resetDatabaseInnhold() {
        repository.deleteAll()
    }

    @Test
    fun `findByPayloadAndType - skal finne task for gitt payload og type`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.FEILET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = UUID.randomUUID().toString(), status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.UBEHANDLET)

        repository.save(ubehandletTask)
        repository.save(feiletTask1)
        repository.save(feiletTask2)

        val funnetTask = repository.findByPayloadAndType(feiletTask1.payload, TaskStep2.TASK_2)

        assertThat(funnetTask?.payload).isEqualTo(feiletTask1.payload)
        assertThat(funnetTask?.type).isEqualTo(feiletTask1.type)
        assertThat(funnetTask?.status).isEqualTo(feiletTask1.status)
    }

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
        assertThat(alleTasks.size).isEqualTo(3 + preCount.size)
        assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2 + preCountFeilet)
        assertThat(alleTasks.count { it.status == Status.UBEHANDLET }).isEqualTo(1 + preCountUbehandlet)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker gitt en status`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)

        repository.save(ubehandletTask)
        repository.save(feiletTask1)
        repository.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(listOf(Status.FEILET), PageRequest.of(0, 1000))
        assertThat(alleTasks.size).isEqualTo(2)
        assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2)
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
        assertThat(alleTasks.size).isEqualTo(1)
        assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(1)
    }

    @Test
    fun `findByStatus - skall returnere tasks sortert etter opprettet_tid`() {
        val task1 = Task(type = TaskStep1.TASK_1, payload = "1", opprettetTid = LocalDateTime.now())
        val task2 = Task(type = TaskStep2.TASK_2, payload = "2", opprettetTid = LocalDateTime.now().minusDays(1))
        val task3 = Task(type = TaskStep2.TASK_2, payload = "3", opprettetTid = LocalDateTime.now().plusDays(1))

        repository.save(task1)
        repository.save(task2)
        repository.save(task3)

        val message = repository.findByStatusIn(listOf(Status.UBEHANDLET),
                                                PageRequest.of(0, 3, Sort.Direction.DESC, "opprettetTid"))
        assertThat(message.map { it.payload }).containsExactly("3", "1", "2")
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
        assertThat(task.metadata.getProperty(property)).isEqualTo(property)
    }

    @Test
    internal fun `skal håndtere optimistic locking`() {
        val task = repository.save(Task(TaskStep1.TASK_1, "{'a'='b'}"))
        TestTransaction.flagForCommit()
        TestTransaction.end()
        TestTransaction.start()
        repository.save(task.copy(status = Status.KLAR_TIL_PLUKK))
        TestTransaction.flagForCommit()
        TestTransaction.end()
        assertThat(catchThrowable { repository.save(task.copy(status = Status.KLAR_TIL_PLUKK)) })
                .matches { isOptimisticLocking(it as Exception) }
    }

}
