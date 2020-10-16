package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.cumulative.CumulativeCounter
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.FeilendeTaskStep1
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@ActiveProfiles("mock-task-step-service")
@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest
class ScheduledTasksServiceIntegrationTest {

    @Autowired
    private lateinit var scheduledTasksService: ScheduledTaskService

    @Autowired
    private lateinit var taskStepExecutorService: TaskStepExecutorService

    @Autowired
    private lateinit var taskStepService: TaskStepService

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Test
    fun `skal ikke rekjøre feilede tasker som oppnått max antall feilet`() {
        val feilteller = CumulativeCounter(mockk())
        every { taskStepService.finnFeilteller(any()) } returns feilteller
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.isolationLevel = TransactionDefinition.ISOLATION_REPEATABLE_READ;
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW;

        val nyTask = transactionTemplate.execute {
            taskRepository.save(Task(FeilendeTaskStep1.FEILENDE_TASK_1, "{'a'='b'}"))
        }
        val id = nyTask.id

        /**
         * Vid første og andre kjøringen (default feil på en task er 3) så skal den settes til KLAR_TIL_PLUKK på nytt
         */
        repeat(2) {
            taskStepExecutorService.pollAndExecute()
            scheduledTasksService.retryFeilendeTask()
        }
        assertThat(feilteller.count()).isEqualTo(0.0)
        assertThat(taskRepository.findByIdOrNull(id)!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)

        /**
         * Vid tredje kjøringen skal den feile tasken slik att den ikke fortsetter å rekjøre
         * Den skal også registrere 1 feil til feiltellern
         * Vid fortsatt kjøringer skal den fortsatt ligge som feilet
         */
        repeat(3) {
            taskStepExecutorService.pollAndExecute()
            assertThat(taskRepository.findByIdOrNull(id)!!.status).isEqualTo(Status.FEILET)
            assertThat(feilteller.count()).isEqualTo(1.0)

            scheduledTasksService.retryFeilendeTask()
            val task = taskRepository.findByIdOrNull(id)!!
            assertThat(task.status).isEqualTo(Status.FEILET)
            assertThat(task.logg.filter { it.type == Loggtype.FEILET }).hasSize(3)
        }
    }

}