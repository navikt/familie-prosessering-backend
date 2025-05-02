package no.nav.familie.prosessering

import io.mockk.every
import io.mockk.spyk
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@SpringBootConfiguration
@EnableJdbcRepositories("no.nav.familie")
@ComponentScan("no.nav.familie")
class TestAppConfig : JdbcRepositoryConfigExtension() {
    @Bean
    fun prosesseringInfoProvider() =
        object : ProsesseringInfoProvider {
            override fun hentBrukernavn(): String = "id"

            override fun harTilgang(): Boolean = true

            override fun isLeader(): Boolean = true
        }

    /**
     * Mocker ut TaskStepExecutor, hvis ikke kjører den runnables async
     */
    @Primary
    @Bean(name = ["taskExecutor"])
    fun threadPoolTaskExecutor(): TaskExecutor {
        val taskExecutor1 =
            spyk<TaskExecutor>(
                ThreadPoolTaskExecutor().also {
                    it.initialize()
                },
            )
        every { taskExecutor1.execute(any()) } answers {
            firstArg<Runnable>().run()
        }
        return taskExecutor1
    }

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)
}
