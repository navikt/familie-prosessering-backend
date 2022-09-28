package no.nav.familie.prosessering.domene

enum class Status {
    FERDIG, FEILET, PLUKKET, BEHANDLER, KLAR_TIL_PLUKK, UBEHANDLET, AVVIKSHÅNDTERT, MANUELL_OPPFØLGING, FERDIG_NÅ_FEILET_FØR;

    fun kanPlukkes() = listOf(KLAR_TIL_PLUKK, UBEHANDLET).contains(this)
}
