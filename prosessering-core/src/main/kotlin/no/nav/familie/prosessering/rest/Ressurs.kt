package no.nav.familie.prosessering.rest

data class Ressurs<T>(
    val data: T?,
    val status: Status,
    val melding: String,
    val frontendFeilmelding: String? = null,
) {
    enum class Status {
        SUKSESS,
        FEILET,
    }

    companion object {
        fun <T> success(data: T): Ressurs<T> =
            Ressurs(
                data = data,
                status = Status.SUKSESS,
                melding = "Innhenting av data var vellykket",
            )

        fun <T> failure(
            errorMessage: String? = null,
            error: Throwable? = null,
        ): Ressurs<T> =
            Ressurs(
                data = null,
                status = Status.FEILET,
                melding = errorMessage ?: "En feil har oppstått: ${error?.message}",
                frontendFeilmelding = errorMessage ?: "En feil har oppstått: ${error?.message}",
            )
    }

    override fun toString(): String {
        return "Ressurs(status=$status, melding='$melding')"
    }
}
