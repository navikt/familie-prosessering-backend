package no.nav.familie.prosessering.sikkerhet

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!dev")
internal class ProsesseringTaskApiSecurityConfig(
    private val prosesseringJwtAuthenticationConverter: ProsesseringJwtAuthenticationConverter,
) {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun prosesseringTaskApiChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/api/task/**")
            authorizeHttpRequests {
                authorize("/api/task/**", authenticated)
            }
            oauth2ResourceServer {
                jwt {
                    jwtAuthenticationConverter = prosesseringJwtAuthenticationConverter
                }
            }
            csrf { disable() }
        }

        return http.build()
    }
}
