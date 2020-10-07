package no.nav.familie.prosessering.domene.jdbc

import no.nav.familie.prosessering.domene.Status
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
interface TaskRepository : PagingAndSortingRepository<Task, Long> {

    fun findByStatusInAndTriggerTidBeforeOrderByOpprettetTidDesc(status: List<Status>,
                                                                 triggerTid: LocalDateTime,
                                                                 page: Pageable): List<Task>

    fun findByStatus(status: Status): List<Task>

    fun findByStatusIn(status: List<Status>, page: Pageable): List<Task>

    fun findByStatusAndTriggerTidBefore(status: Status, triggerTid: LocalDateTime): List<Task>
}
