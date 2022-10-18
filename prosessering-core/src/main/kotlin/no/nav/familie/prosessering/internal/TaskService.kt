package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.domene.AntallÅpneTask
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TaskService(val taskRepository: TaskRepository) {

    fun findById(id: Long): Task {
        return taskRepository.findByIdOrNull(id) ?: error("Task med id: $id ikke funnet.")
    }

    fun save(task: Task): Task {
        return taskRepository.save(task)
    }

    fun finnAlleTasksKlareForProsessering(page: Pageable): List<Task> {
        return taskRepository.findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(
            listOf(
                Status.KLAR_TIL_PLUKK,
                Status.UBEHANDLET
            ),
            LocalDateTime.now(),
            page
        )
    }

    fun finnAlleFeiledeTasks(): List<Task> {
        return taskRepository.findByStatus(Status.FEILET)
    }

    fun finnAllePlukkedeTasks(): List<Task> {
        return taskRepository.findByStatus(Status.PLUKKET)
    }

    fun finnTasksMedStatus(status: List<Status>, page: Pageable): List<Task> {
        return taskRepository.findByStatusIn(status, page)
    }

    fun finnTasksKlarForSletting(eldreEnnDato: LocalDateTime, page: Pageable): Page<Task> {
        return taskRepository.findByStatusAndTriggerTidBefore(Status.FERDIG, eldreEnnDato, page)
    }

    fun finnTasksTilFrontend(status: List<Status>, page: Pageable, type: String? = null): List<Task> {
        return if (type == null) taskRepository.findByStatusIn(status, page)
        else taskRepository.findByStatusInAndType(status, type, page)
    }

    fun finnTasksSomErFerdigNåMenFeiletFør(page: Pageable): List<Task> =
        taskRepository.finnTasksSomErFerdigNåMenFeiletFør(page)

    fun finnTaskMedPayloadOgType(payload: String, type: String): Task? {
        return taskRepository.findByPayloadAndType(payload, type)
    }

    fun delete(it: Task) {
        taskRepository.delete(it)
    }

    fun antallTaskerTilOppfølging(): Long {
        return taskRepository.countByStatusIn(listOf(Status.MANUELL_OPPFØLGING, Status.FEILET))
    }

    fun tellAntallÅpneTasker(): List<AntallÅpneTask> {
        return taskRepository.countOpenTasks()
    }
}
