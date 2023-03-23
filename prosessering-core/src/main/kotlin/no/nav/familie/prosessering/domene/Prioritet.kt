package no.nav.familie.prosessering.domene

enum class Prioritet(val verdi: Int) {
    LAV(-1),
    NORMAL(0),
    HØY(1);

    companion object {
        val prioritetPåVerdi = values().associateBy { it.verdi }

        fun fraVerdi(verdi: Int): Prioritet {
            return prioritetPåVerdi.getValue(verdi)
        }
    }
}