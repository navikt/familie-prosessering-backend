package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLoggRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TaskLoggRepositoryTest : IntegrationRunnerTest() {
    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var taskLoggRepository: TaskLoggRepository

    @Test
    internal fun `skal hente metadata til task`() {
        var task = taskService.save(Task("minType", "payload"))
        task = taskService.feilet(task, TaskFeil(task, RuntimeException("Feil")), 3, 3, false)
        task = taskService.ferdigstill(task)
        task = taskService.kommenter(task, "kommentar på task", "", false)

        val taskLoggMetadata = taskLoggRepository.finnTaskLoggMetadata(listOf(task.id)).single()
        assertThat(taskLoggMetadata.taskId).isEqualTo(task.id)
        assertThat(taskLoggMetadata.antallLogger).isEqualTo(4)
        assertThat(taskLoggMetadata.sisteKommentar).isEqualTo("kommentar på task")

        val taskLogger = taskLoggRepository.findByTaskId(task.id)
        val sistOpprettetTid = taskLogger.filterNot { it.type == Loggtype.KOMMENTAR }.maxOf { it.opprettetTid }
        assertThat(taskLoggMetadata.sistOpprettetTid).isEqualTo(sistOpprettetTid)
    }
}
