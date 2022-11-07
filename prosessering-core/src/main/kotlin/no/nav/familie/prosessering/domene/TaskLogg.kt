package no.nav.familie.prosessering.domene

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("task_logg")
data class TaskLogg(
    @Id
    val id: Long = 0L,
    val taskId: Long,
    val endretAv: String = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES,
    val type: Loggtype,
    val node: String = "node1",
    val melding: String? = null,
    val opprettetTid: LocalDateTime = LocalDateTime.now()
) {

    override fun toString(): String {
        return "TaskLogg(id=$id, type=$type, opprettetTid=$opprettetTid)"
    }

    companion object {
        const val BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"
    }
}

data class TaskLoggMetadata(
    val taskId: Long,
    val antallLogger: Int,
    val sistOpprettetTid: LocalDateTime,
    val sisteKommentar: String?
)
