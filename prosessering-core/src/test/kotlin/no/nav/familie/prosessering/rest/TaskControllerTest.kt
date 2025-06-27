package no.nav.familie.prosessering.rest

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.task.TaskStep1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

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

    @Test
    fun `feilmelding er riktig når alle type tasker rekjøres`() {
        every { taskService.finnTasksMedStatus(any(), any(), any()) } throws RuntimeException()

        val response = taskController.rekjørTasks(Status.FEILET)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.data).isNull()
        assertThat(response.body!!.melding).isEqualTo("Rekjøring av tasker med status '${Status.FEILET}' feilet")
    }

    @Test
    fun `feilmelding er riktig når tasker med gitt type rekjøres`() {
        every { taskService.finnTasksMedStatus(any(), any(), any()) } throws RuntimeException()

        val response = taskController.rekjørTasks(Status.FEILET, TaskStep1.TASK_1)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.data).isNull()
        assertThat(
            response.body!!.melding,
        ).isEqualTo("Rekjøring av tasker med status '${Status.FEILET}' og type '${TaskStep1.TASK_1}' feilet")
    }
}
