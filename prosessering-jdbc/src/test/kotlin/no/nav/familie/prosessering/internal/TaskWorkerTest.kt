package no.nav.familie.prosessering.internal

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.util.isOptimisticLocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
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
import java.util.concurrent.atomic.AtomicBoolean

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
class TaskWorkerTest {

    @SpykBean
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

    @Test
    internal fun `skal håndtere 2 prosesser som oppdaterer en task`() {
        val called = AtomicBoolean(false)
        val task = repository.save(Task(TaskStep1.TASK_1, "{'a'='b'}"))
        TestTransaction.flagForCommit()
        TestTransaction.end()

        every { repository.save(any()) } answers {
            if (called.compareAndSet(false, true)) {
                Thread.sleep(1000)
            }
            callOriginal()
        }
        val first = Thread {
            val exception = catchThrowable { worker.markerPlukket(task.id) } as Exception
            assertThat(isOptimisticLocking(exception)).isTrue
        }

        val second = Thread {
            Thread.sleep(200)
            worker.markerPlukket(task.id)
        }
        first.start()
        second.start()
        first.join()
        second.join()
    }

}
