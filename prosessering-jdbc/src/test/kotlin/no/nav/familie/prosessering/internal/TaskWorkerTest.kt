package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.transaction.TestTransaction

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
class TaskWorkerTest {

    @Autowired
    private lateinit var repository: TaskRepository

    @Autowired
    private lateinit var worker: TaskWorker

    @AfterEach
    fun clear() {
        repository.deleteAll()
    }

    @Test
    fun `skal behandle task`() {
        val task = Task(TaskStep1.TASK_1, "{'a'='b'}").plukker()
        val savedTask = repository.save(task)
        assertThat(task.status).isEqualTo(Status.PLUKKET)
        TestTransaction.flagForCommit()
        TestTransaction.end()
        worker.doActualWork(savedTask.id)
        val findByIdOrNull = repository.findByIdOrNull(savedTask.id)
        assertThat(findByIdOrNull?.status).isEqualTo(Status.FERDIG)
        assertThat(findByIdOrNull?.logg).hasSize(4)
    }

}
