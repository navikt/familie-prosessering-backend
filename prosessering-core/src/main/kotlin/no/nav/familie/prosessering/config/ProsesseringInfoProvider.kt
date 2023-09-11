package no.nav.familie.prosessering.config

interface ProsesseringInfoProvider {

    /**
     * Brukes for logging av ident som gjør requester
     */
    fun hentBrukernavn(): String

    /**
     * Verifiserer at bruker har tilgang til å kalle på task-api'et, eks gjennom å sjekke roller
     */
    fun harTilgang(): Boolean

    /**
     * Noen deler kan være fint å kun kjøre på leader, eks sletting av tasks for å unngå locks
     * Hvis man ikke har leader election, returner true
     */
    fun isLeader(): Boolean
}
