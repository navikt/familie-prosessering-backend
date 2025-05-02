package no.nav.familie.prosessering.config

import no.nav.familie.prosessering.IntegrationRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.io.IOException

class KotlinTransactionalTest : IntegrationRunnerTest() {
    @Autowired
    private lateinit var service: KotlinTransactionalTestService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS TEST_TABLE")
        jdbcTemplate.execute("CREATE TABLE TEST_TABLE (id SERIAL PRIMARY KEY, data VARCHAR(255))")
    }

    @Test
    fun `happy case`() {
        service.lagre("testverdi")

        assertThat(hentAntallRader()).isEqualTo(1)
    }

    @Test
    fun `skal rulle tilbake når checked exception IOException kastes`() {
        try {
            service.lagreMedFeil("testverdi")
        } catch (_: Exception) {
            // Forventet unntak, ingen handling nødvendig
        }

        assertThat(hentAntallRader()).isEqualTo(0)
    }

    private fun hentAntallRader(): Int? = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TEST_TABLE", Int::class.java)
}

@Service
class KotlinTransactionalTestService(
    private val jdbcTemplate: JdbcTemplate,
) {
    @KotlinTransactional(propagation = Propagation.REQUIRES_NEW)
    fun lagre(data: String) {
        jdbcTemplate.update("INSERT INTO TEST_TABLE (data) VALUES (?)", data)
    }

    @KotlinTransactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreMedFeil(data: String) {
        jdbcTemplate.update("INSERT INTO TEST_TABLE (data) VALUES (?)", data)
        throw IOException("Simulert feil")
    }
}
