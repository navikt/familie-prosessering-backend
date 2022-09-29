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

    @Test
    fun tomListeGirTomtResultat() {
        assertThat(
            TaskService(
                taskRepository = mockk<TaskRepository>().also {
                    every {
                        it.findByStatusIn(
                            any(),
                            any()
                        )
                    } returns listOf()
                }
            ).finnTasksSomErFerdigNåMenFeiletFør(
                page = Pageable.unpaged()
            )
        ).isEmpty()
    }

    @Test
    fun listeForFerdigGirFerdige() {
        assertThat(
            TaskService(
                taskRepository = mockk<TaskRepository>()
                    .also {
                        every {
                            it.findByStatusIn(
                                not(eq(listOf(Status.FERDIG))),
                                any()
                            )
                        } returns listOf()
                    }
                    .also {
                        every {
                            it.findByStatusIn(
                                eq(listOf(Status.FERDIG)),
                                any()
                            )
                        } returns listOf(mockk())
                    }
            ).finnTasksTilFrontend(
                status = listOf(Status.FERDIG),
                page = Pageable.unpaged()
            )
        ).hasSize(1)
    }

    @Test
    fun listeForFerdigNåFeiletFørGirFerdigeMedFleireEnnFireLogginnslag() {
        val ferdigOK = mockk<ITask>().also { every { it.logg } returns listOf(mockk(), mockk(), mockk(), mockk()) }
        val ferdigNåFeiletFør =
            mockk<ITask>().also { every { it.logg } returns listOf(mockk(), mockk(), mockk(), mockk(), mockk()) }
        assertThat(
            TaskService(
                taskRepository = mockk<TaskRepository>()
                    .also {
                        every {
                            it.findByStatusIn(
                                not(eq(listOf(Status.FERDIG))),
                                any()
                            )
                        } returns listOf()
                    }
                    .also {
                        every {
                            it.findByStatusIn(
                                eq(listOf(Status.FERDIG)),
                                any()
                            )
                        } returns listOf(ferdigOK, ferdigNåFeiletFør)
                    }
            ).finnTasksSomErFerdigNåMenFeiletFør(
                page = Pageable.unpaged()
            )
        ).hasSize(1)
    }
}
