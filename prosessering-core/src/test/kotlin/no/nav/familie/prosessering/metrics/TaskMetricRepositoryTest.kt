package no.nav.familie.prosessering.metrics

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TaskMetricRepositoryTest : IntegrationRunnerTest() {

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskMetricRepository: TaskMetricRepository

    @Test
    fun `skal beregne riktig`() {
        taskService.save(Task("type", "payload").copy(status = Status.FERDIG))
        taskService.save(Task("type", "payload2").copy(status = Status.FEILET))
        taskService.save(Task("type2", "payload2").copy(status = Status.FEILET))
        taskService.save(Task("type", "payload3").copy(status = Status.MANUELL_OPPFØLGING))
        taskService.save(Task("type", "payload4").copy(status = Status.MANUELL_OPPFØLGING))

        val expected = listOf(
            AntallTaskAvTypeOgStatus("type", Status.FEILET, 1),
            AntallTaskAvTypeOgStatus("type2", Status.FEILET, 1),
            AntallTaskAvTypeOgStatus("type", Status.MANUELL_OPPFØLGING, 2),
        )
        assertThat(taskMetricRepository.finnAntallFeiledeTasksPerTypeOgStatus()).containsExactlyInAnyOrderElementsOf(expected)
    }
}
