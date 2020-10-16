package no.nav.familie.prosessering

import io.mockk.spyk
import no.nav.familie.prosessering.internal.TaskStepService
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension

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

    @Bean
    @Profile("mock-task-step-service")
    @Primary
    fun taskStepService(taskStepTyper: List<AsyncTaskStep>): TaskStepService = spyk(TaskStepService(taskStepTyper))

}
