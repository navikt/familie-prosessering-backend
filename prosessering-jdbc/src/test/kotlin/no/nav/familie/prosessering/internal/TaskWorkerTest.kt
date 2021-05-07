package no.nav.familie.prosessering.internal

import com.ninjasquad.springmockk.SpykBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.justRun
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
            println("Yolo")
            if(called.compareAndSet(false, true)){
                println("sleep")
                Thread.sleep(500)
                println("sleep done")
            } else {
                println("do not sleep")
            }
            callOriginal()
        }
        runBlocking {
            val update1 = async {
                println("1")
                worker.markerPlukket(task.id)
                println("1 - done")
            }
            val update2 = async {
                println("2")
                worker.markerPlukket(task.id)
                println("2 - done")
            }
            update1.await()
            update2.await()
        }
    }
}
