package no.nav.familie.prosessering.sikkerhet

import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class ProsesseringUserAuthorizationFilter(
    private val påkrevdRolle: String,
    private val oidcUtil: OIDCUtil,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        when {
            ourIssuer() == null -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No value for `ourIssuer`")
            currentUserGroups() == null -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No user-groups in JWT")
            !currentUserGroups()!!.contains(påkrevdRolle) -> response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Missing group $påkrevdRolle in JWT",
            )
            else -> filterChain.doFilter(request, response)
        }
    }

    /*
     * Skal kun kjøre på for /api/task
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return !path.startsWith("/api/task")
    }

    private fun ourIssuer() = oidcUtil.getClaimAsList("groups")
    private fun currentUserGroups() = ourIssuer()
}
