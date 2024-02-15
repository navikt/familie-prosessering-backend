package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLoggRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TaskServiceIntegrationTest : IntegrationRunnerTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var taskLoggRepository: TaskLoggRepository

    @Test
    internal fun `skal lage og hente metadata til task`() {
        var task = taskService.save(Task("minType", "payload"))
        task = taskService.kommenter(task, "kommentar på task", "", false)
        task = taskService.feilet(task, TaskFeil(task, RuntimeException("Feil")), 3, 3, false)
        task = taskService.ferdigstill(task)

        val taskLoggMetadata = taskLoggRepository.finnTaskLoggMetadata(listOf(task.id)).single()
        assertThat(taskLoggMetadata.taskId).isEqualTo(task.id)
        assertThat(taskLoggMetadata.antallLogger).isEqualTo(4)
        assertThat(taskLoggMetadata.sisteKommentar).isEqualTo("kommentar på task")
    }

    @Test
    internal fun `skal sette task-status til MANUELL_OPPFØLGING og lage nytt logginnslag`() {
        val task = taskService.save(Task(type = "minType", payload = "payload"))
        val årsak = "årsak"

        val oppdatertTask = taskService.settTilManuellOppfølging(task = task, årsak = årsak)

        assertThat(oppdatertTask.status).isEqualTo(Status.MANUELL_OPPFØLGING)
        val taskLogg = taskLoggRepository.findByTaskId(oppdatertTask.id).find { it.type == Loggtype.MANUELL_OPPFØLGING }
        assertThat(taskLogg).isNotNull()
        assertThat(taskLogg?.melding).isEqualTo(årsak)
    }
}
