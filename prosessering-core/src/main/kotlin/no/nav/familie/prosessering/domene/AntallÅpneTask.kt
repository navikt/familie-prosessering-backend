package no.nav.familie.prosessering.domene

interface AntallÅpneTask {
    val type: String
    val status: Status
    val count: Long
}