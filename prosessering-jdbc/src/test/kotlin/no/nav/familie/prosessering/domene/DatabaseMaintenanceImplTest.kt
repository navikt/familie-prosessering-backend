package no.nav.familie.prosessering.domene

import no.nav.familie.prosessering.IntegrationRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class DatabaseMaintenanceImplTest : IntegrationRunnerTest() {

    @Autowired
    private lateinit var databaseMaintenance: DatabaseMaintenance

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Test
    internal fun `skal slette eldre tasks`() {
        val etÅrSiden = LocalDateTime.now().minusYears(1)
        val task1 = taskRepository.save(opprettTask(triggerTid = etÅrSiden))
        val task2 = taskRepository.save(opprettTask(triggerTid = etÅrSiden))

        val task3 = taskRepository.save(opprettTask(triggerTid = etÅrSiden, status = Status.FEILET))
        val task4 = taskRepository.save(opprettTask())

        taskRepository.findAll().forEach { println(it) }

        val slettedeTasks = databaseMaintenance.slettFerdigstilteTasksFørTidspunkt(LocalDateTime.now().minusDays(1))

        assertThat(slettedeTasks.map { it.id }).containsExactlyInAnyOrder(task1.id, task2.id)
        assertThat(taskRepository.findAll().map { it.id }).containsExactlyInAnyOrder(task3.id, task4.id)
    }

    private fun opprettTask(status: Status = Status.FERDIG, triggerTid: LocalDateTime = LocalDateTime.now()) =
        Task(
            type = "type",
            payload = UUID.randomUUID().toString(),
            status = status,
            opprettetTid = triggerTid,
            triggerTid = triggerTid
        )
}
