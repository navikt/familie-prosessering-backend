package no.nav.familie.prosessering.internal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.transaction.TestTransaction

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest
class TaskStepExecutorServiceTest {

    @Autowired
    private lateinit var repository: TaskRepository

    @MockkBean(relaxUnitFun = true)
    lateinit var taskStep: TaskStep2

    @Autowired
    private lateinit var taskStepExecutorService: TaskStepExecutorService

    @Test
    fun `skal håndtere feil`() {
        val task = Task(TaskStep2.TASK_2, "{'a'='b'}")
        val savedTask = repository.save(task)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(savedTask.status).isEqualTo(Status.UBEHANDLET)
        every { taskStep.doTask(any()) } throws (IllegalStateException())

        taskStepExecutorService.pollAndExecute()
        val taskKlarTilPlukk = repository.findById(savedTask.id).orElseThrow()
        assertThat(taskKlarTilPlukk.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(taskKlarTilPlukk.logg).hasSize(3)

        taskStepExecutorService.pollAndExecute()
        assertThat(repository.findById(savedTask.id).orElseThrow().status).isEqualTo(Status.KLAR_TIL_PLUKK)

        taskStepExecutorService.pollAndExecute()
        assertThat(repository.findById(savedTask.id).orElseThrow().status).isEqualTo(Status.FEILET)
    }

    @Test
    fun `skal håndtere samtidighet`() {
        repeat(100) {
            val task2 = Task(TaskStep2.TASK_2, "{'a'='b'}")
            repository.save(task2)
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()

        runBlocking {
            val launch = GlobalScope.launch {
                repeat(10) { taskStepExecutorService.pollAndExecute() }
            }
            val launch2 = GlobalScope.launch {
                repeat(10) { taskStepExecutorService.pollAndExecute() }
            }

            launch.join()
            launch2.join()
        }
        val findAll = repository.findAll()
        findAll.filter { it.status != Status.FERDIG || it.logg.size > 4 }.forEach {
            assertThat(it.status).isEqualTo(Status.FERDIG)
            assertThat(it.logg.size).isEqualTo(4)
        }

    }


}