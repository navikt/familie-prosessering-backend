package no.nav.familie.prosessering.domene

import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
@Primary
interface TaskRepository : ITaskRepostitory<Task> {
    @Query("""select t.type,t.status, count(*) as count from task t WHERE t.status in ('UBEHANDLET', 'BEHANDLER', 'PLUKKET', 'KLAR_TIL_PLUKK') GROUP BY t.type, t.status""")
    override fun countOpenTasks(): List<AntallÅpneTask>

    @Query("""select distinct t.id from task t join task_logg tl on t.id = tl.task_id WHERE t.status = 'FERDIG' and tl.type = 'FEILET'""")
    override fun finnTasksSomErFerdigNåMenFeiletFør(page: Pageable): List<Long>
}
