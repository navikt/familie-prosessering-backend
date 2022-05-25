package no.nav.familie.prosessering.domene

data class AntallÅpneTask(
    val type: String,
    val status: Status,
    val count: Long,
)