package no.nav.familie.prosessering.error

import java.time.LocalDateTime

data class RekjørSenereException(val årsak: String, val triggerTid: LocalDateTime) :
    RuntimeException("Rekjører senere - triggerTid=$triggerTid")

data class MaxAntallRekjøringerException(val maxAntallRekjøring: Int) :
    RuntimeException("Nådd max antall rekjøring - $maxAntallRekjøring")
