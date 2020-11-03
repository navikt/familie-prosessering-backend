package no.nav.familie.prosessering.domene

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("task_logg")
data class TaskLogg(@Id
                    override val id: Long = 0L,
                    override val endretAv: String = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES,
                    override val type: Loggtype,
                    override val node: String = "node1",
                    override val melding: String? = null,
                    override val opprettetTid: LocalDateTime = LocalDateTime.now()) : ITaskLogg()