package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.TaskLoggRepository
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DbContainerInitializer::class, TestAppConfig::class])
@DataJdbcTest(excludeAutoConfiguration = [TestDatabaseAutoConfiguration::class])
abstract class IntegrationRunnerTest {
    @Autowired
    private lateinit var repository: TaskRepository

    @Autowired
    private lateinit var taskLoggRepository: TaskLoggRepository

    @AfterEach
    fun clear() {
        taskLoggRepository.deleteAll()
        repository.deleteAll()
    }
}
