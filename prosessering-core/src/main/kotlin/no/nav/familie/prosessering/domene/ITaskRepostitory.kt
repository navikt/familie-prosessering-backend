package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.LocalDateTime

@NoRepositoryBean
interface ITaskRepostitory<T : ITask> : PagingAndSortingRepository<T, Long> {

    fun findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(status: List<Status>,
                                                                 triggerTid: LocalDateTime,
                                                                 page: Pageable): List<T>

    fun findByStatus(status: Status): List<T>

    fun findByStatusIn(status: List<Status>, page: Pageable): List<T>

    fun findByStatusAndTriggerTidBefore(status: Status, triggerTid: LocalDateTime): List<T>

    fun countByStatusIn(status: List<Status>): Long

    fun findByStatusInAndType(status: List<Status>, type: String, page: Pageable): List<T>

    fun findByPayloadAndType(payload: String, type: String): T
}