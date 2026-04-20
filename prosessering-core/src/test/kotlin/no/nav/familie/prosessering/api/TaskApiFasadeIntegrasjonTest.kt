package no.nav.familie.prosessering.api

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.rest.KommentarDTO
import no.nav.familie.prosessering.rest.PaginableResponse
import no.nav.familie.prosessering.rest.RestTaskService
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

internal class TaskApiFasadeIntegrasjonTest : IntegrationRunnerTest() {
    @Autowired
    lateinit var restTaskService: RestTaskService

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var repository: TaskRepository

    private lateinit var api: TaskApiFasade

    @BeforeEach
    fun setup() {
        val infoProvider =
            object : ProsesseringInfoProvider {
                override fun hentBrukernavn(): String = ""

                override fun harTilgang(): Boolean = true
            }
        api = TaskApiFasade(restTaskService, infoProvider)
    }

    @Test
    fun `skal bare rekjøre tasker status FEILET`() {
        var ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        var taskSomSkalRekjøres = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        var avvikshåndtert = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT)
        ubehandletTask = taskService.save(ubehandletTask)
        taskSomSkalRekjøres = taskService.save(taskSomSkalRekjøres)
        avvikshåndtert = taskService.save(avvikshåndtert)

        api.rekjørTasks(Status.FEILET, null)

        assertThat(repository.findById(taskSomSkalRekjøres.id).get().status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(repository.findById(ubehandletTask.id).get().status).isEqualTo(Status.UBEHANDLET)
        assertThat(repository.findById(avvikshåndtert.id).get().status).isEqualTo(Status.AVVIKSHÅNDTERT)
    }

    @Test
    fun `skal kjøre tasker for alle typer når type-parameter er null`() {
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "", status = Status.FEILET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "", status = Status.FEILET))

        api.rekjørTasks(Status.FEILET, null)

        val tasks = repository.findAll()
        assertThat(tasks).hasSize(2).allSatisfy {
            assertThat(it.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        }
    }

    @Test
    fun `skal kjøre tasker for alle typer når type-parameter er tom string`() {
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "", status = Status.FEILET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "", status = Status.FEILET))

        api.rekjørTasks(Status.FEILET, "")

        val tasks = repository.findAll()
        assertThat(tasks).hasSize(2).allSatisfy {
            assertThat(it.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        }
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, mode = EnumSource.Mode.EXCLUDE, names = ["FEILET"])
    fun `skal bare rekjøre tasker med gitt status og type`(status: Status) {
        val task1Feilet = taskService.save(Task(type = TaskStep1.TASK_1, payload = "", status = Status.FEILET))
        val task1AnnenStatus = taskService.save(Task(type = TaskStep1.TASK_1, payload = "", status = status))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "", status = Status.FEILET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "", status = status))

        api.rekjørTasks(Status.FEILET, TaskStep1.TASK_1)

        val tasks = repository.findAll()
        assertThat(tasks).hasSize(4)
        assertThat(tasks.single { it.id == task1Feilet.id }.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(tasks.single { it.id == task1AnnenStatus.id }.status).isEqualTo(status)
        assertThat(tasks.filter { it.type == TaskStep2.TASK_2 }.map { it.status }).contains(Status.FEILET, status)
    }

    @Test
    fun `skal finne riktig antall tasker for oppfølging`() {
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.MANUELL_OPPFØLGING))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FERDIG))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.KLAR_TIL_PLUKK))

        val response = api.finnAntallTaskerSomKreverOppfølging()

        assertThat(response.data).isEqualTo(2)
    }

    @Test
    fun `skal finne riktig antall tasker som har feilet og ligger til manuell oppfølging`() {
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))
        taskService.saveAll(
            listOf(
                Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET),
                Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET),
                Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET),
            ),
        )
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.MANUELL_OPPFØLGING))

        val response = api.finnAntallTaskerMedStatusFeiletOgManuellOppfølging()

        assertThat(response.data?.antallFeilet).isEqualTo(3)
        assertThat(response.data?.antallManuellOppfølging).isEqualTo(1)
    }

    @Test
    fun `skal hente tasker av angitt type`() {
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.MANUELL_OPPFØLGING))
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='1'}", status = Status.FERDIG))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.KLAR_TIL_PLUKK))

        val response = api.hentTasks(null, null, TaskStep1.TASK_1)

        val data = response.data as PaginableResponse<*>
        assertThat(data.tasks).hasSize(2)
    }

    @Test
    fun `skal hente kommentarer hvis det finnes i logg`() {
        val lagretTask = taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))

        api.kommenterTask(lagretTask.id, KommentarDTO(false, "dette er en test"))

        val response = api.hentTasks(null, null, TaskStep1.TASK_1)
        val dto = ((response.data as PaginableResponse<*>).tasks).first() as no.nav.familie.prosessering.rest.TaskDto

        assertThat(dto.kommentar).isEqualTo("dette er en test")
    }

    @Test
    fun `skal kommentere og sette status til manuell oppfølging`() {
        val lagretTask = taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))

        api.kommenterTask(lagretTask.id, KommentarDTO(true, "dette er en test"))

        val response = api.hentTasks(null, null, TaskStep1.TASK_1)
        val dto = ((response.data as PaginableResponse<*>).tasks).first() as no.nav.familie.prosessering.rest.TaskDto

        assertThat(dto.kommentar).isEqualTo("dette er en test")
        assertThat(dto.status).isEqualTo(Status.MANUELL_OPPFØLGING)
    }

    @Test
    fun `skal hente alle tasktyper`() {
        taskService.save(Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET))
        taskService.save(Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.AVVIKSHÅNDTERT))

        val response = api.hentAlleTasktyper()

        assertThat(response.data).hasSize(2).contains(TaskStep1.TASK_1, TaskStep2.TASK_2)
    }
}
