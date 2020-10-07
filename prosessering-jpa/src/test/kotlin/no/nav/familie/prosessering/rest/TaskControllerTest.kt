package no.nav.familie.prosessering.rest

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepositoryProxyJpa
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test


internal class TaskControllerTest {

    private val taskRepository: TaskRepositoryProxyJpa = mockk()

    private lateinit var restTaskService: RestTaskService
    private lateinit var taskController: TaskController

    @Before
    fun setup() {
        restTaskService = RestTaskService(taskRepository)
        taskController = TaskController(restTaskService, mockk())
        every { taskController.hentBrukernavn() } returns ""

    }

    @Test
    fun `skal hente task basert på alle statuser`() {
        val statusSlot = slot<List<Status>>()
        every { taskRepository.finnTasksMedStatus(capture(statusSlot), any()) } returns emptyList()

        taskController.task(null, null)
        Assertions.assertThat(statusSlot.captured).isEqualTo(Status.values().toList())
    }

    @Test
    fun `skal hente task basert på en status`() {
        val statusSlot = slot<List<Status>>()
        every { taskRepository.finnTasksMedStatus(capture(statusSlot), any()) } returns emptyList()

        taskController.task(Status.FEILET, null)
        Assertions.assertThat(statusSlot.captured).isEqualTo(listOf(Status.FEILET))
    }
}
