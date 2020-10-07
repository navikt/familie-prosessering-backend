package no.nav.familie.prosessering.domene

import java.time.LocalDateTime


abstract class ITaskLogg {

    abstract val id: Long

    abstract val endretAv: String

    abstract val type: Loggtype

    abstract val node: String

    abstract val melding: String?

    abstract val opprettetTid: LocalDateTime

    override fun toString(): String {
        return """TaskLogg(id=$id, 
            |endretAv=$endretAv, 
            |type=$type, 
            |node='$node', 
            |melding=$melding, 
            |opprettetTidspunkt=$opprettetTid)""".trimMargin()
    }

    companion object {

        const val BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"
    }
}