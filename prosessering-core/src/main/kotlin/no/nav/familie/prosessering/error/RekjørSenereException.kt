package no.nav.familie.prosessering.error

import java.time.LocalDateTime

data class RekjørSenereException(val årsak: String, val triggerTid: LocalDateTime) :
    RuntimeException("Rekjører senere - triggerTid=$triggerTid")
