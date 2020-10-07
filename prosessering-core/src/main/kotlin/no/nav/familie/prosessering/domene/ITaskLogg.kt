package no.nav.familie.prosessering.domene

import java.time.LocalDateTime


abstract class ITaskLogg {

    abstract val id: Long

    abstract val endretAv: String

    abstract val type: Loggtype

    abstract val node: String

    abstract val melding: String?

    abstract val opprettetTid: LocalDateTime

    companion object {

        const val BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"
    }
}