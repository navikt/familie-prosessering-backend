package no.nav.familie.prosessering.error

/**
 * Noen ganger har man ikke behov for stacktrace når en feil er håndtert inne i en `AsyncTaskStep`
 * Hvis en task rekjører mange ganger genereres veldig mye stacktrace.
 * Denne stacktracen vises også i prosessering-frontend.
 *
 */
class TaskExceptionUtenStackTrace(
    melding: String,
) : RuntimeException(melding)
