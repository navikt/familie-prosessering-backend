package no.nav.familie.prosessering.domene

import no.nav.familie.prosessering.TestAppConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
internal class TaskStatistikkRepositoryTest {

    @Autowired private lateinit var repository: TaskRepository
    @Autowired private lateinit var statistikkRepository: TaskStatistikkRepository

    @AfterEach
    fun clear() {
        repository.deleteAll()
    }

    @Test
    internal fun `skal hente status fra statistikk`() {
        repository.save(Task("Test", "payload"))
        assertThat(statistikkRepository.hentStatus()).isEqualTo(listOf(Status.UBEHANDLET to 1).toMap())
    }
}