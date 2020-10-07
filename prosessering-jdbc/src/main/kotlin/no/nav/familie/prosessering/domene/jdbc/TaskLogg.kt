package no.nav.familie.prosessering.domene.jdbc

import no.nav.familie.prosessering.domene.ITaskLogg
import no.nav.familie.prosessering.domene.Loggtype
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("TASK_LOGG")
data class TaskLogg(override val id: Long = 0L,
                    override val endretAv: String = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES,
                    override val type: Loggtype,
                    override val node: String = "node1",
                    override val melding: String? = null,
                    override val opprettetTid: LocalDateTime = LocalDateTime.now()) : ITaskLogg()