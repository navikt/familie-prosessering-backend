package no.nav.familie.prosessering.domene

import java.time.LocalDateTime

data class TaskTilSletting(val id: Long, val type: String, val callId: String, val triggerTid: LocalDateTime)
