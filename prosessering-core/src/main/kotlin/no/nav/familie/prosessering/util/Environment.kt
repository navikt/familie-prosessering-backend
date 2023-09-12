package no.nav.familie.prosessering.util

internal object Environment {
    fun hentLeaderSystemEnv(): String? {
        return System.getenv("ELECTOR_PATH") ?: return null
    }
}
