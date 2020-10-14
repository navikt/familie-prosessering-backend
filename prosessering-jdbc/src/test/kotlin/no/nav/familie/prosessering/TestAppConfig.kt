package no.nav.familie.prosessering

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
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

}
