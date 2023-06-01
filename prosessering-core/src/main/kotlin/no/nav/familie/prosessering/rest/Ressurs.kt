package no.nav.familie.prosessering.rest

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Objekt som brukes for utveksling av data mellom familietjenester.
 * Brukes både mellom systemer og til frontend.
 *
 * MERK Dette er en (redusert) duplikat av klassen som ligger i familie-kontrakter:felles
 * Duplisert for å redusere antall avhengigheter som følger med prosessering-core
 *
 * @param T typen til data i objektet.
 * @param status status på request. Kan være 200 OK med feilet ressurs
 * @param melding teknisk melding som ikke skal inneholde sensitive data
 * @param stacktrace stacktrace fra feil som kan være nyttig til debugging i familie-prosessering
 *
 *
 */
data class Ressurs<T>(
    val data: T?,
    val status: Status,
    val melding: String,
    val stacktrace: String?
) {

    enum class Status {
        SUKSESS,
        FEILET,
    }

    companion object {
        fun <T> success(data: T): Ressurs<T> = Ressurs(
            data = data,
            status = Status.SUKSESS,
            melding = "Innhenting av data var vellykket",
            stacktrace = null,
        )

        fun <T> failure(
            errorMessage: String? = null,
            error: Throwable? = null,
        ): Ressurs<T> = Ressurs(
            data = null,
            status = Status.FEILET,
            melding = errorMessage ?: "En feil har oppstått: ${error?.message}",
            stacktrace = error?.textValue(),
        )

        private fun Throwable.textValue(): String {
            val sw = StringWriter()
            this.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }

    override fun toString(): String {
        return "Ressurs(status=$status, melding='$melding')"
    }
}
