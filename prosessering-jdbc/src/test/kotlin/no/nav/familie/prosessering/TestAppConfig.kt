package no.nav.familie.prosessering

import io.mockk.every
import io.mockk.spyk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@SpringBootConfiguration
@EnableJdbcRepositories("no.nav.familie")
@ComponentScan("no.nav.familie")
class TestAppConfig : JdbcRepositoryConfigExtension() {

    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder {
        return object : TokenValidationContextHolder {
            override fun getTokenValidationContext(): TokenValidationContext {
                return TokenValidationContext(emptyMap())
            }

            override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext) {}
        }
    }

    /**
     * Mocker ut TaskStepExecutor, hvis ikke kjører den runnables async
     */
    @Primary
    @Bean(name=["taskExecutor"])
    fun threadPoolTaskExecutor(): TaskExecutor {
        val taskExecutor1 = spyk<TaskExecutor>(ThreadPoolTaskExecutor().also {
            it.initialize()
        })
        every { taskExecutor1.execute(any()) } answers {
            firstArg<Runnable>().run()
        }
        return taskExecutor1
    }

}
