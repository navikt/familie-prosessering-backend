package no.nav.familie.prosessering.sikkerhet

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authorization.AuthenticatedAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationManagers
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
internal class ProsesseringTaskApiSecurityConfig(
    @param:Value("\${AZURE_OPENID_CONFIG_ISSUER}") private val issuerUri: String,
    @param:Value("\${AZURE_APP_CLIENT_ID}") private val acceptedAudience: String,
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun prosesseringTaskApiChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/api/task/**")
            authorizeHttpRequests {
                // Keep task-api access check scoped to /api/task/**.
                val harTilgang =
                    AuthorizationManager<RequestAuthorizationContext> { _, _ ->
                        AuthorizationDecision(prosesseringInfoProvider.harTilgang())
                    }

                authorize(
                    anyRequest,
                    AuthorizationManagers.allOf(AuthenticatedAuthorizationManager.authenticated(), harTilgang),
                )
            }
            oauth2ResourceServer {
                jwt {
                    // Configure issuer/audience validation only for /api/task/**.
                    jwtDecoder =
                        ProsesseringJwtDecoder(
                            issuerUri = issuerUri,
                            acceptedAudience = acceptedAudience,
                        )
                }
            }
            csrf { disable() }
        }

        return http.build()
    }
}
