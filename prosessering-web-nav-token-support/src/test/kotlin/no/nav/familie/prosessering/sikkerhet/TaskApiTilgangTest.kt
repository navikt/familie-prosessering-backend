package no.nav.familie.prosessering.sikkerhet

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.prosessering.api.TaskApiFasade
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.rest.Ressurs
import no.nav.familie.prosessering.rest.TaskControllerNavTokenSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    classes = [TaskApiTilgangTest.TestConfig::class],
    webEnvironment = RANDOM_PORT,
)
class TaskApiTilgangTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var prosesseringInfoProvider: ProsesseringInfoProvider

    @Autowired
    private lateinit var taskApiFasade: TaskApiFasade

    private val httpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun setUp() {
        clearMocks(prosesseringInfoProvider, taskApiFasade)
    }

    @Test
    fun `skal slippe gjennom kall til task-api når bruker har tilgang`() {
        // Arrange
        every { prosesseringInfoProvider.harTilgang() } returns true
        every { taskApiFasade.hentAlleTasktyper() } returns Ressurs.success(listOf("type"))

        // Act
        val response = get("/api/task/type/alle")

        // Assert
        assertThat(response.statusCode()).isEqualTo(200)
        verify(exactly = 1) { taskApiFasade.hentAlleTasktyper() }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/api/task/type/alle",
            "/api/%74ask/type/alle",
            "/%61pi/task/type/alle",
            "/api;a=b/task/type/alle",
        ],
    )
    fun `skal gi 401 for kall til task-api når bruker mangler tilgang`(path: String) {
        // Arrange
        every { prosesseringInfoProvider.harTilgang() } returns false

        // Act
        val response = get(path)

        // Assert
        assertThat(response.statusCode()).isEqualTo(401)
        verify(exactly = 0) { taskApiFasade.hentAlleTasktyper() }
    }

    @Test
    fun `skal ikke sjekke tilgang til tasker for andre endepunkter`() {
        // Arrange
        every { prosesseringInfoProvider.harTilgang() } returns false

        // Act
        val response = get("/api/annet")

        // Assert
        assertThat(response.statusCode()).isEqualTo(200)
        verify(exactly = 0) { prosesseringInfoProvider.harTilgang() }
    }

    @Test
    fun `skal ikke sjekke tilgang til tasker når en tidligere interceptor har avvist kallet`() {
        // Arrange
        every { prosesseringInfoProvider.harTilgang() } returns true

        // Act
        val response = get("/api/task/type/alle", AVVIS_HEADER to "true")

        // Assert
        assertThat(response.statusCode()).isEqualTo(403)
        verify(exactly = 0) { prosesseringInfoProvider.harTilgang() }
    }

    private fun get(
        path: String,
        vararg headers: Pair<String, String>,
    ): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET()
        headers.forEach { (navn, verdi) -> request.header(navn, verdi) }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("no.nav.familie.prosessering.sikkerhet")
    @Import(TaskControllerNavTokenSupport::class)
    class TestConfig : WebMvcConfigurer {
        @Bean
        fun prosesseringInfoProvider(): ProsesseringInfoProvider = mockk()

        @Bean
        fun taskApiFasade(): TaskApiFasade = mockk()

        override fun addInterceptors(registry: InterceptorRegistry) {
            registry.addInterceptor(AvvisendeInterceptor()).order(Ordered.LOWEST_PRECEDENCE - 1)
        }
    }

    class AvvisendeInterceptor : HandlerInterceptor {
        override fun preHandle(
            request: HttpServletRequest,
            response: HttpServletResponse,
            handler: Any,
        ): Boolean {
            if (request.getHeader(AVVIS_HEADER) == null) {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }
    }

    @RestController
    class AnnetController {
        @GetMapping("/api/annet")
        fun annet(): String = "annet"
    }

    companion object {
        private const val AVVIS_HEADER = "Avvis"
    }
}
