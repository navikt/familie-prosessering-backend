package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface TaskRepositoryProxy {

    fun findById(id: Long): ITask

    fun save(task: ITask): ITask

    fun finnAlleTasksKlareForProsessering(page: Pageable): List<ITask>

    fun finnAlleFeiledeTasks(): List<ITask>

    fun finnTasksMedStatus(status: List<Status>, page: Pageable): List<ITask>

    fun finnTasksKlarForSletting(eldreEnnDato: LocalDateTime): List<ITask>

    fun finnTasksTilFrontend(status: List<Status>, page: Pageable): List<ITask>

    fun delete(it: ITask)
}