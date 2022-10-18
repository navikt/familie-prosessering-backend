package no.nav.familie.prosessering.rest

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

internal class TaskControllerIntegrasjonTest : IntegrationRunnerTest() {

    @Autowired
    lateinit var restTaskService: RestTaskService

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var repository: TaskRepository

    lateinit var taskController: TaskController

    @BeforeEach
    fun setup() {
        taskController = TaskController(restTaskService, mockk())
        every { taskController.hentBrukernavn() } returns ""
    }

    @Test
    fun `skal bare rekjøre tasker status FEILET`() {
        var ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        var taskSomSkalRekjøres = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        var avvikshåndtert = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT)
        ubehandletTask = taskService.save(ubehandletTask)
        taskSomSkalRekjøres = taskService.save(taskSomSkalRekjøres)
        avvikshåndtert = taskService.save(avvikshåndtert)

        val response = taskController.rekjørTasks(Status.FEILET)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(repository.findById(taskSomSkalRekjøres.id).get().status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(repository.findById(ubehandletTask.id).get().status).isEqualTo(Status.UBEHANDLET)
        assertThat(repository.findById(avvikshåndtert.id).get().status).isEqualTo(Status.AVVIKSHÅNDTERT)
    }

    @Test
    fun `skal finne riktig antall tasker for oppfølging`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        val feiletTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val avvikshåndtertTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT)
        val manuellOppfølgingTask =
            Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.MANUELL_OPPFØLGING)
        val ferdigTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FERDIG)
        val klarTilPlukkTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.KLAR_TIL_PLUKK)
        taskService.save(ubehandletTask)
        taskService.save(feiletTask)
        taskService.save(avvikshåndtertTask)
        taskService.save(manuellOppfølgingTask)
        taskService.save(ferdigTask)
        taskService.save(klarTilPlukkTask)

        val response = taskController.antallTilOppfølging()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body.data).isEqualTo(2)
    }

    @Test
    fun `skal hente tasker av angitt type`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        val feiletTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val avvikshåndtertTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT)
        val manuellOppfølgingTask =
            Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.MANUELL_OPPFØLGING)
        val ferdigTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='1'}", status = Status.FERDIG)
        val klarTilPlukkTask = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.KLAR_TIL_PLUKK)
        taskService.save(ubehandletTask)
        taskService.save(feiletTask)
        taskService.save(avvikshåndtertTask)
        taskService.save(manuellOppfølgingTask)
        taskService.save(ferdigTask)
        taskService.save(klarTilPlukkTask)

        val response = taskController.task2(null, null, TaskStep1.TASK_1)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat((response.body.data as PaginableResponse<*>).tasks).hasSize(2)
    }

    @Test
    fun `skal hente kommentarer hvis det finnes i logg`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)

        val lagretTask = taskService.save(ubehandletTask)

        taskController.kommenterTask(lagretTask.id, KommentarDTO(false, "dette er en test"))

        val response = taskController.task2(null, null, TaskStep1.TASK_1)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat((((response.body.data as PaginableResponse<*>).tasks).first() as TaskDto).kommentar.equals("dette er en test"))
    }

    @Test
    fun `skal kommentere og sette status til manuell oppfølging`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)

        val lagretTask = taskService.save(ubehandletTask)

        taskController.kommenterTask(lagretTask.id, KommentarDTO(true, "dette er en test"))

        val response = taskController.task2(null, null, TaskStep1.TASK_1)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat((((response.body.data as PaginableResponse<*>).tasks).first() as TaskDto).kommentar.equals("dette er en test"))
        assertThat((((response.body.data as PaginableResponse<*>).tasks).first() as TaskDto).status.equals(Status.MANUELL_OPPFØLGING))
    }
}
