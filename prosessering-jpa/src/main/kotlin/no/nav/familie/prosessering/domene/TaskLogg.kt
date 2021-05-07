package no.nav.familie.prosessering.domene

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "TASK_LOGG")
data class TaskLogg(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_logg_seq")
        @SequenceGenerator(name = "task_logg_seq")
        override val id: Long = 0L,

        override val endretAv: String = BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES,

        @Enumerated(EnumType.STRING)
        override val type: Loggtype,

        override val node: String = "node1",

        @Column(name = "melding", updatable = false, columnDefinition = "text")
        override val melding: String? = null,

        override val opprettetTid: LocalDateTime = LocalDateTime.now()
) : ITaskLogg() {

    override fun toString(): String {
        return "TaskLogg(id=$id, type=$type, opprettetTid=$opprettetTid)"
    }
}
