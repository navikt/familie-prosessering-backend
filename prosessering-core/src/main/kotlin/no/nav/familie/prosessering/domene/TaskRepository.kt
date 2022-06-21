package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TaskRepository : PagingAndSortingRepository<Task, Long> {

    fun findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(
        status: List<Status>,
        triggerTid: LocalDateTime,
        page: Pageable
    ): List<Task>

    fun findByStatus(status: Status): List<Task>

    fun findByStatusIn(status: List<Status>, page: Pageable): List<Task>

    fun findByStatusAndTriggerTidBefore(status: Status, triggerTid: LocalDateTime): List<Task>

    fun countByStatusIn(status: List<Status>): Long

    fun findByStatusInAndType(status: List<Status>, type: String, page: Pageable): List<Task>

    fun findByPayloadAndType(payload: String, type: String): Task?

    @Query("""select t.type,t.status, count(*) as count from task t WHERE t.status in ('UBEHANDLET', 'BEHANDLER', 'PLUKKET', 'KLAR_TIL_PLUKK') GROUP BY t.type, t.status""")
    fun countOpenTasks(): List<AntallÅpneTask>
}
