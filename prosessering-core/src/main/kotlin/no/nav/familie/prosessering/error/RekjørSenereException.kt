package no.nav.familie.prosessering.error

import java.time.LocalDateTime

/**
 * Kan brukes hvis man ønsker at en task skal feile nå og ikke rekjøre direkte.
 * Eks hvis tasken feilet flere ganger en kveld og man ønsker å rekjøre den neste arbeidsdag kl 7
 */
data class RekjørSenereException(
    val årsak: String,
    val triggerTid: LocalDateTime,
) : RuntimeException("Rekjører senere - triggerTid=$triggerTid")

data class MaxAntallRekjøringerException(
    val maxAntallRekjøring: Int,
) : RuntimeException("Nådd max antall rekjøring - $maxAntallRekjøring")
