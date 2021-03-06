package no.nav.familie.prosessering.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep2
import no.nav.familie.prosessering.task.TaskStepExceptionUtenStackTrace
import no.nav.familie.prosessering.task.TaskStepFeilManuellOppfølgning
import no.nav.familie.prosessering.task.TaskStepRekjørSenere
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
import java.time.LocalDate
import java.util.*

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
class TaskStepExecutorServiceTest {

    @Autowired
    private lateinit var repository: TaskRepository

    @MockkBean(relaxUnitFun = true)
    lateinit var taskStep: TaskStep2

    @Autowired
    private lateinit var taskStepExecutorService: TaskStepExecutorService

    @AfterEach
    fun clear() {
        repository.deleteAll()
    }

    @Test
    fun `skal håndtere feil`() {
        val savedTask = repository.save(Task(TaskStep2.TASK_2, "{'a'='b'}"))
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

    @Test
    fun `settTilManuellOppfølgning=true - skal sette en task til manuell oppfølgning når den feilet 3 ganger`() {
        val task = repository.save(Task(TaskStepFeilManuellOppfølgning.TASK_FEIL_1, "{'a'='b'}"))
        TestTransaction.flagForCommit()
        TestTransaction.end()

        repeat(2) {
            taskStepExecutorService.pollAndExecute()
            assertThat(repository.findByIdOrNull(task.id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        }

        taskStepExecutorService.pollAndExecute()

        val feiletTask = repository.findByIdOrNull(task.id)!!
        assertThat(feiletTask.status).isEqualTo(Status.MANUELL_OPPFØLGING)
        assertThat(om.readValue<TaskFeil>(feiletTask.logg.last().melding!!).stackTrace).isNotNull
    }

    @Test
    internal fun `rekjørSenere - skal rekjøre tasks som kaster RekjørSenereException`() {
        val task = repository.save(Task(TaskStepRekjørSenere.TYPE, UUID.randomUUID().toString()))
        TestTransaction.flagForCommit()
        TestTransaction.end()

        taskStepExecutorService.pollAndExecute()

        val lagretTask = repository.findByIdOrNull(task.id)!!
        assertThat(lagretTask.triggerTid).isEqualTo(LocalDate.of(2088, 1, 1).atStartOfDay())
        assertThat(lagretTask.status).isEqualTo(Status.KLAR_TIL_PLUKK)
    }

    @Test
    internal fun `skal ikke lagre stack trace hvis det ikke trengs`() {
        val task = repository.save(Task(TaskStepExceptionUtenStackTrace.TYPE, UUID.randomUUID().toString()))
        TestTransaction.flagForCommit()
        TestTransaction.end()

        taskStepExecutorService.pollAndExecute()

        val oppdatertTask = repository.findByIdOrNull(task.id)!!

        assertThat(oppdatertTask.logg).hasSize(3)
        val melding = om.readValue<TaskFeil>(oppdatertTask.logg.toList()[2].melding!!)
        assertThat(melding.feilmelding).isEqualTo("feilmelding")
        assertThat(melding.stackTrace).isEqualTo(null)
    }

    companion object {
        val om = ObjectMapper()
    }
}
