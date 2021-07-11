package no.nav.familie.prosessering.domene

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class TaskStatistikkRepository(private val jdbcTemplate: JdbcTemplate) {

    fun hentStatus(): Map<Status, Int> {
        return jdbcTemplate.query("SELECT status, COUNT(*) cn FROM task GROUP BY status") { rs, _ ->
            Status.valueOf(rs.getString("status")) to rs.getInt("cn")
        }.toMap()
    }
}