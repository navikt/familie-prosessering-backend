package no.nav.familie.prosessering.domene

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
internal interface TaskLoggRepository : PagingAndSortingRepository<TaskLogg, Long> {

    @Query
    fun findByTaskId(taskId: Long): List<TaskLogg>

    @Query
    fun findByTaskIdIn(taskId: List<Long>): List<TaskLogg>

    @Query(
        """
        SELECT task_id, count(*) antall_logger, MAX(opprettet_tid) sist_opprettet_tid, 
            (SELECT melding FROM task_logg tl1 
              WHERE tl1.task_id = tl.task_id AND type='KOMMENTAR' ORDER BY tl1.opprettet_tid DESC LIMIT 1) siste_kommentar
        FROM task_logg tl
        WHERE task_id IN (:taskIds)
        GROUP BY task_id
    """,
    )
    fun finnTaskLoggMetadata(taskIds: List<Long>): List<TaskLoggMetadata>

    fun countByTaskIdAndType(taskId: Long, type: Loggtype): Int

    @Modifying
    @Query("""DELETE FROM task_logg WHERE task_id = :taskId""")
    fun deleteAllByTaskId(taskId: Long)

    @Modifying
    @Query("""DELETE FROM task_logg WHERE task_id in (:taskId)""")
    fun deleteAllByTaskIdIn(taskId: List<Long>)
}
