package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.domene.AntallÅpneTask
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TaskService(val taskRepository: TaskRepository) {

    fun findById(id: Long): ITask {
        return taskRepository.findByIdOrNull(id) ?: error("Task med id: $id ikke funnet.")
    }

    fun save(task: ITask): ITask {
        return taskRepository.save(task)
    }

    fun finnAlleTasksKlareForProsessering(page: Pageable): List<ITask> {
        return taskRepository.findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(
            listOf(
                Status.KLAR_TIL_PLUKK,
                Status.UBEHANDLET
            ),
            LocalDateTime.now(),
            page
        )
    }

    fun finnAlleFeiledeTasks(): List<ITask> {
        return taskRepository.findByStatus(Status.FEILET)
    }

    fun finnAllePlukkedeTasks(): List<ITask> {
        return taskRepository.findByStatus(Status.PLUKKET)
    }

    fun finnTasksMedStatus(status: List<Status>, page: Pageable): List<ITask> {
        return taskRepository.findByStatusIn(status, page)
    }

    fun finnTasksKlarForSletting(eldreEnnDato: LocalDateTime, page: Pageable): Page<ITask> {
        return taskRepository.findByStatusAndTriggerTidBefore(Status.FERDIG, eldreEnnDato, page)
    }

    fun finnTasksTilFrontend(status: List<Status>, page: Pageable, type: String? = null): List<ITask> {
        if (status == listOf(Status.FERDIG_NÅ_FEILET_FØR)) {
            val tasks = if (type == null) taskRepository.findByStatusIn(listOf(Status.FERDIG), page)
            else taskRepository.findByStatusInAndType(status, type, page)
            return tasks.filter { it.logg.size > 4 }
        }
        return if (type == null) taskRepository.findByStatusIn(status, page)
        else taskRepository.findByStatusInAndType(status, type, page)
    }

    fun finnTaskMedPayloadOgType(payload: String, type: String): ITask? {
        return taskRepository.findByPayloadAndType(payload, type)
    }

    fun delete(it: ITask) {
        taskRepository.delete(it)
    }

    fun antallTaskerTilOppfølging(): Long {
        return taskRepository.countByStatusIn(listOf(Status.MANUELL_OPPFØLGING, Status.FEILET))
    }

    fun tellAntallÅpneTasker(): List<AntallÅpneTask> {
        return taskRepository.countOpenTasks()
    }
}
