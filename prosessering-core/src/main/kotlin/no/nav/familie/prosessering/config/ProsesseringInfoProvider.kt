package no.nav.familie.prosessering.config

import no.nav.familie.prosessering.util.LeaderClient

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
     * null blir håndtert som true
     */
    fun isLeader(): Boolean? = LeaderClient.isLeader()
}
