package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TaskRepository : JpaRepository<Task, Long> {

    fun findByStatusInAndTriggerTidBeforeOrderByOpprettetTidDesc(status: List<Status>,
                                                                 triggerTid: LocalDateTime,
                                                                 page: Pageable): List<Task>

    fun findByStatus(status: Status): List<Task>

    fun findByStatusIn(status: List<Status>, page: Pageable): List<Task>

    fun findByStatusAndTriggerTidBefore(status: Status, triggerTid: LocalDateTime): List<Task>

}
