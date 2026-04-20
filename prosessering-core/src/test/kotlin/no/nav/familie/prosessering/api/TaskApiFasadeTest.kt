package no.nav.familie.prosessering.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.rest.RestTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TaskApiFasadeTest {
    private val taskService: TaskService = mockk()

    private lateinit var restTaskService: RestTaskService
    private lateinit var api: TaskApiFasade

    @BeforeEach
    fun setup() {
        restTaskService = RestTaskService(taskService)
        val infoProvider = mockk<ProsesseringInfoProvider>()
        every { infoProvider.hentBrukernavn() } returns ""
        api = TaskApiFasade(restTaskService, infoProvider)
    }

    @Test
    fun `skal hente task basert på alle statuser`() {
        val statusSlot = slot<List<Status>>()
        every { taskService.finnTasksMedStatus(capture(statusSlot), any(), any()) } returns emptyList()
        every { taskService.finnTaskLoggMetadata(any()) } returns emptyMap()

        api.hentTasks(null, null, null)

        assertThat(statusSlot.captured).isEqualTo(Status.values().toList())
    }

    @Test
    fun `skal hente task basert på en status`() {
        val statusSlot = slot<List<Status>>()
        every { taskService.finnTasksMedStatus(capture(statusSlot), any(), any()) } returns emptyList()
        every { taskService.finnTaskLoggMetadata(any()) } returns emptyMap()

        api.hentTasks(Status.FEILET, null, null)

        assertThat(statusSlot.captured).isEqualTo(listOf(Status.FEILET))
    }

    @Test
    fun `feilmelding er riktig når alle type tasker rekjøres`() {
        every { taskService.finnTasksMedStatus(any(), anyNullable<String>(), any()) } throws RuntimeException("boom")

        val response = api.rekjørTasks(Status.FEILET, null)

        assertThat(response.data).isNull()
        assertThat(response.melding).isEqualTo("Rekjøring av tasker med status '${Status.FEILET}' feilet")
    }

    @Test
    fun `feilmelding er riktig når tasker med gitt type rekjøres`() {
        every { taskService.finnTasksMedStatus(any(), anyNullable<String>(), any()) } throws RuntimeException("boom")

        val response = api.rekjørTasks(Status.FEILET, "task1")

        assertThat(response.data).isNull()
        assertThat(response.melding).isEqualTo("Rekjøring av tasker med status '${Status.FEILET}' og type 'task1' feilet")
    }
}
