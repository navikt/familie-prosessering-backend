package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TaskRepository : PagingAndSortingRepository<ITask, Long> {

    fun findByStatusInAndTriggerTidBeforeOrderByOpprettetTidDesc(status: List<Status>,
                                                                 triggerTid: LocalDateTime,
                                                                 page: Pageable): List<ITask>

    fun findByStatus(status: Status): List<ITask>

    fun findByStatusIn(status: List<Status>, page: Pageable): List<ITask>

    fun findByStatusAndTriggerTidBefore(status: Status, triggerTid: LocalDateTime): List<ITask>

}
