package no.nav.familie.prosessering

import io.mockk.every
import io.mockk.spyk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@SpringBootConfiguration
@EnableJpaRepositories("no.nav.familie")
@EntityScan("no.nav.familie")
@ComponentScan("no.nav.familie")
class TestAppConfig : JpaRepositoryConfigExtension() {

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
