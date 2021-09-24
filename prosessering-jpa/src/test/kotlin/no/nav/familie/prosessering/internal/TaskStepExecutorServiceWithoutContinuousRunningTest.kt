package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStepMedFeilMedTriggerTid0
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.transaction.TestTransaction

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class],
             properties = ["prosessering.continuousRunning.enabled=false"])
class TaskStepExecutorServiceWithoutContinuousRunningTest {

    @Autowired
    private lateinit var repository: TaskRepository

    @Autowired
    private lateinit var taskStepExecutorService: TaskStepExecutorService

    @AfterEach
    fun clear() {
        repository.deleteAll()
    }

    @Test
    fun `skal håndtere feil i en ny schedulering med continuousRunning=false`() {
        var savedTask = Task(TaskStepMedFeilMedTriggerTid0.TYPE, "{'a'='b'}")
        repository.save(savedTask)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(savedTask.status).isEqualTo(Status.UBEHANDLET)

        taskStepExecutorService.pollAndExecute()
        savedTask = repository.findById(savedTask.id).orElseThrow()
        assertThat(savedTask.status).isEqualTo(Status.KLAR_TIL_PLUKK)

        taskStepExecutorService.pollAndExecute()
        savedTask = repository.findById(savedTask.id).orElseThrow()
        assertThat(savedTask.status).isEqualTo(Status.KLAR_TIL_PLUKK)

        taskStepExecutorService.pollAndExecute()
        savedTask = repository.findById(savedTask.id).orElseThrow()
        assertThat(savedTask.status).isEqualTo(Status.FEILET)

        assertThat(savedTask.logg.filter { it.type == Loggtype.FEILET }).hasSize(3)
    }

}
