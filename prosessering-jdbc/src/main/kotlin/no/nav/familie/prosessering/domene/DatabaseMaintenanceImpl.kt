package no.nav.familie.prosessering.domene

import no.nav.familie.log.mdc.MDCConstants
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
class DatabaseMaintenanceImpl(
    private val jdbcTemplate: JdbcTemplate
) : DatabaseMaintenance {

    // language=PostgreSQL
    private val sqlForSletting =
        """
        WITH tasks_til_sletting AS (SELECT id FROM task WHERE status = ? AND trigger_tid < ?), 
        slett_task_logg AS (DELETE FROM task_logg WHERE task_id IN (SELECT id from tasks_til_sletting)) 
        DELETE FROM task WHERE id IN (SELECT id FROM tasks_til_sletting) RETURNING id, type, metadata, trigger_tid
        """

    @Transactional
    override fun slettFerdigstilteTasksFørTidspunkt(tidspunkt: LocalDateTime): List<TaskTilSletting> {
        return jdbcTemplate.query(sqlForSletting, { rs, _ ->
            TaskTilSletting(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("metadata").asProperties().getProperty(MDCConstants.MDC_CALL_ID),
                rs.getTimestamp("trigger_tid").toLocalDateTime()
            )
        }, Status.FERDIG.name, Timestamp.valueOf(tidspunkt))
    }
}
