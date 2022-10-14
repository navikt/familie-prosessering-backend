package no.nav.familie.prosessering.internal

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable

internal class TaskServiceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val service = TaskService(taskRepository)

    @Test
    fun tomListeGirTomtResultat() {
        every { taskRepository.findByStatusIn(any(), any()) } returns listOf()
        assertThat(service.finnTasksSomErFerdigNåMenFeiletFør(Pageable.unpaged())).isEmpty()
    }

    @Test
    fun listeForFerdigGirFerdige() {
        every { taskRepository.findByStatusIn(not(eq(listOf(Status.FERDIG))), any()) } returns listOf()
        every { taskRepository.findByStatusIn(eq(listOf(Status.FERDIG)), any()) } returns listOf(mockk())

        assertThat(service.finnTasksTilFrontend(listOf(Status.FERDIG), Pageable.unpaged()))
            .hasSize(1)
    }

    @Test
    fun listeForFerdigNåFeiletFørGirFerdigeMedFleireEnnFireLogginnslag() {
        val ferdigOK = mockk<ITask>().also { every { it.logg } returns listOf(mockk(), mockk(), mockk(), mockk()) }
        val ferdigNåFeiletFør =
            mockk<ITask>().also { every { it.logg } returns listOf(mockk(), mockk(), mockk(), mockk(), mockk()) }

        every { taskRepository.findByStatusIn(not(eq(listOf(Status.FERDIG))), any()) } returns listOf()
        every { taskRepository.findByStatusIn(eq(listOf(Status.FERDIG)), any()) } returns
            listOf(ferdigOK, ferdigNåFeiletFør)

        assertThat(service.finnTasksSomErFerdigNåMenFeiletFør(Pageable.unpaged())).hasSize(1)
    }
}
