package no.nav.familie.prosessering.util.leader

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.familie.prosessering.util.Environment
import no.nav.familie.prosessering.util.LeaderClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress

class LeaderClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(Environment::class)
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
        unmockkStatic(Environment::class)
    }

    @Test
    fun `Skal returnere null hvis environement variable ELECTOR_PATH ikke eksisterer`() {
        every { Environment.hentLeaderSystemEnv() } returns null
        assertThat(LeaderClient.isLeader()).isNull()
    }

    @Test
    fun `Skal returnere true hvis pod er leader`() {
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(
            get(anyUrl())
                .willReturn(aResponse().withBody("{\"name\": \"${InetAddress.getLocalHost().hostName}\"}")),
        )

        assertThat(LeaderClient.isLeader()).isTrue()
    }

    @Test
    fun `Skal returnere false hvis pod ikke er leader`() {
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(get(anyUrl()).willReturn(aResponse().withBody("foobar")))

        assertFalse(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere null hvis response er tom`() {
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(
            get(anyUrl()).willReturn(aResponse().withStatus(404)),
        )

        assertThat(LeaderClient.isLeader()).isNull()
    }

    @Test
    fun `Skal returnere null hvis kallet feilet`() {
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(
            get(anyUrl()).willReturn(serverError()),
        )

        assertThat(LeaderClient.isLeader()).isNull()
    }

    @Test
    fun `Skal returnere null når requesten tar for lang tid`() {
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"

        wireMockServer.stubFor(
            get(anyUrl()).willReturn(aResponse().withBody("foobar").withFixedDelay(4000)),
        )

        assertThat(LeaderClient.isLeader()).isFalse()
    }
}
