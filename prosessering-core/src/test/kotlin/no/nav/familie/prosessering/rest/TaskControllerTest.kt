package no.nav.familie.prosessering.rest

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TaskControllerTest {
    private val taskService: TaskService = mockk()

    private lateinit var restTaskService: RestTaskService
    private lateinit var taskController: TaskController

    @BeforeEach
    fun setup() {
        restTaskService = RestTaskService(taskService)
        taskController = TaskController(restTaskService, mockk())
        every { taskController.hentBrukernavn() } returns ""
    }

    @Test
    fun `skal hente task basert på alle statuser`() {
        val statusSlot = slot<List<Status>>()
        every { taskService.finnTasksMedStatus(capture(statusSlot), any(), any()) } returns emptyList()

        taskController.task2(null, null)
        assertThat(statusSlot.captured).isEqualTo(Status.values().toList())
    }

    @Test
    fun `skal hente task basert på en status`() {
        val statusSlot = slot<List<Status>>()
        every { taskService.finnTasksMedStatus(capture(statusSlot), any(), any()) } returns emptyList()

        taskController.task2(Status.FEILET, null)
        assertThat(statusSlot.captured).isEqualTo(listOf(Status.FEILET))
    }
}
