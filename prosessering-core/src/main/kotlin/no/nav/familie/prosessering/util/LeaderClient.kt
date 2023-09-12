package no.nav.familie.prosessering.util

import no.nav.familie.prosessering.util.Environment.hentLeaderSystemEnv
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal object LeaderClient {

    private val client = HttpClient.newHttpClient()

    private val electorPath = hentLeaderSystemEnv()

    private val request = HttpRequest.newBuilder()
        .uri(URI.create("http://$electorPath"))
        .GET()
        .build()

    private val handler = HttpResponse.BodyHandlers.ofString()

    fun isLeader(): Boolean? {
        val response: HttpResponse<String> = client.send(request, handler)

        val body = response.body()
        if (body.isNullOrBlank()) return null

        return body.contains(InetAddress.getLocalHost().hostName)
    }
}
