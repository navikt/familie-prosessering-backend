package no.nav.familie.prosessering.domene

enum class Prioritet(val verdi: Int) {
    IKKE_VIKTIG(0),
    LITE_VIKTIG(1),
    NORMAL(2),
    VIKTIG(3),
    KRITISK(4),
    ;

    companion object {
        val prioritetPåVerdi = values().associateBy { it.verdi }

        fun fraVerdi(verdi: Int): Prioritet {
            return prioritetPåVerdi.getValue(verdi)
        }
    }
}
