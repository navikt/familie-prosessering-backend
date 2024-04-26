package no.nav.familie.prosessering.metrics

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface TaskMetricRepository : org.springframework.data.repository.Repository<Task, String> {

    @Query(
        """SELECT t.type, t.status, count(t.id) as count FROM task t
                    WHERE t.status IN ('FEILET', 'MANUELL_OPPFØLGING')
                    GROUP by t.type, t.status""",
    )
    fun finnAntallFeiledeTasksPerTypeOgStatus(): List<AntallTaskAvTypeOgStatus>
}

data class AntallTaskAvTypeOgStatus(val type: String, val status: Status, val count: Long)
