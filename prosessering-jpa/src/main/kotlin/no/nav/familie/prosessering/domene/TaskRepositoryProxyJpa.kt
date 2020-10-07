package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TaskRepositoryProxyJpa(val taskRepository: TaskRepository) : TaskRepositoryProxy {

    override fun findById(id: Long): Task {
        return taskRepository.findByIdOrNull(id) ?: error("Task med id: $id ikke funnet.")
    }

    override fun save(task: ITask): Task {
        return taskRepository.save(task as Task)
    }

    override fun finnAlleTasksKlareForProsessering(page: Pageable): List<Task> {
        return taskRepository.findByStatusInAndTriggerTidBeforeOrderByOpprettetTidDesc(listOf(Status.KLAR_TIL_PLUKK,
                                                                                              Status.UBEHANDLET),
                                                                                       LocalDateTime.now(),
                                                                                       page)
    }

    override fun finnAlleFeiledeTasks(): List<Task> {
        return taskRepository.findByStatus(Status.FEILET)
    }

    override fun finnTasksMedStatus(status: List<Status>, page: Pageable): List<Task> {
        return taskRepository.findByStatusIn(status, page)
    }

    override fun finnTasksKlarForSletting(eldreEnnDato: LocalDateTime): List<Task> {
        return taskRepository.findByStatusAndTriggerTidBefore(Status.FERDIG, eldreEnnDato)
    }

    override fun finnTasksTilFrontend(status: List<Status>, page: Pageable): List<Task> {
        return taskRepository.findByStatusIn(status, page)
    }

    override fun delete(it: ITask) {
        taskRepository.delete(it as Task)
    }

}