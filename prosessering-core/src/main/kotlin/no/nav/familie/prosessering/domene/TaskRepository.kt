package no.nav.familie.prosessering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal interface TaskRepository :
    PagingAndSortingRepository<Task, Long>,
    CrudRepository<Task, Long> {
    fun findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(
        status: List<Status>,
        triggerTid: LocalDateTime,
        page: Pageable,
    ): List<Task>

    fun findByStatus(status: Status): List<Task>

    fun findByStatusIn(
        status: List<Status>,
        page: Pageable,
    ): List<Task>

    @Query("SELECT id FROM task WHERE status='FERDIG' AND trigger_tid < :triggerTid LIMIT :limit")
    fun finnTasksTilSletting(
        triggerTid: LocalDateTime,
        limit: Int,
    ): List<Long>

    fun countByStatusIn(status: List<Status>): Long

    fun findByStatusInAndType(
        status: List<Status>,
        type: String,
        page: Pageable,
    ): List<Task>

    fun findByPayloadAndType(
        payload: String,
        type: String,
    ): Task?

    @Query("SELECT * FROM task t WHERE t.payload=:payload and t.type=:type")
    fun findAllByPayloadAndType(
        payload: String,
        type: String,
    ): List<Task>

    @Query(
        """
        SELECT t.type,t.status, count(*) AS count 
        FROM task t WHERE t.status IN ('UBEHANDLET', 'BEHANDLER', 'PLUKKET', 'KLAR_TIL_PLUKK') 
        GROUP BY t.type, t.status
        """,
    )
    fun countOpenTasks(): List<AntallÅpneTask>

    @Query(
        """
        SELECT DISTINCT t.* 
        FROM task t 
        JOIN task_logg tl ON t.id = tl.task_id 
        WHERE t.status = 'FERDIG' AND tl.type IN ('FEILET', 'MANUELL_OPPFØLGNING')""",
    )
    fun finnTasksSomErFerdigNåMenFeiletFør(page: Pageable): List<Task>

    @Query(
        """
        SELECT * 
        FROM task t 
        WHERE t.metadata like concat('%callId=', concat(:callId::text, '%'))
        ORDER BY t.id DESC""",
    )
    fun finnTaskerMedCallId(callId: String): List<Task>

    @Query(
        """
        WITH q AS (
            SELECT 
                t.id,
                tl.type,
                tl.opprettet_tid,
                row_number() OVER (PARTITION BY t.id ORDER BY tl.opprettet_tid DESC) rn
            FROM task t
            JOIN task_logg tl on t.id = tl.task_id
            WHERE t.status = :status
        )
        SELECT t.*
        FROM task t
          JOIN q t2 ON t.id = t2.id
        WHERE t2.rn = 1
          AND t2.opprettet_tid < :tid
    """,
    )
    fun findAllByStatusAndLastProcessed(
        status: Status,
        tid: LocalDateTime,
    ): List<Task>

    @Query(
        """
        SELECT DISTINCT t.type 
        FROM task t """,
    )
    fun hentAlleTasktyper(): List<String>
}
