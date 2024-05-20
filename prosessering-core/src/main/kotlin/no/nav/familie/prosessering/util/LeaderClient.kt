package no.nav.familie.prosessering.util

import no.nav.familie.prosessering.util.Environment.hentLeaderSystemEnv
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

internal object LeaderClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val TIMEOUT = Duration.ofSeconds(3)

    private val client =
        HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build()

    private val handler = HttpResponse.BodyHandlers.ofString()

    @JvmStatic
    fun isLeader(): Boolean? {
        val electorPath = hentLeaderSystemEnv() ?: return null

        val request =
            HttpRequest.newBuilder()
                .timeout(TIMEOUT)
                .uri(URI.create("http://$electorPath"))
                .GET()
                .build()

        try {
            val response = client.send(request, handler)
            val body = response.body()
            if (response.statusCode() != 200 || body.isNullOrBlank()) {
                logger.warn("isLeader response=${response.statusCode()}")
                return null
            }

            return body.contains(InetAddress.getLocalHost().hostName)
        } catch (e: Exception) {
            logger.warn("Sjekk av isLeader feilet", e)
            return false
        }
    }
}
