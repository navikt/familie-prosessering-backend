package no.nav.familie.prosessering.domene

import java.time.LocalDateTime

interface DatabaseMaintenance {
    fun slettFerdigstilteTasksFørTidspunkt(tidspunkt: LocalDateTime): List<TaskTilSletting>
}
