package no.nav.familie.prosessering.sikkerhet

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ProsesseringUserAuthorizationFilter(
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (prosesseringInfoProvider.harTilgang()) {
            filterChain.doFilter(request, response)
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Har ikke tilgang til tasker")
        }
    }

    /*
     * Skal kun kjøre på for /api/task
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return !path.startsWith("/api/task")
    }
}
