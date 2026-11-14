package no.nav.familie.prosessering.sikkerhet

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
internal class ProsesseringTaskApiTilgangConfig(
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(ProsesseringTaskApiTilgangInterceptor(prosesseringInfoProvider))
            .order(Ordered.LOWEST_PRECEDENCE)
    }
}
