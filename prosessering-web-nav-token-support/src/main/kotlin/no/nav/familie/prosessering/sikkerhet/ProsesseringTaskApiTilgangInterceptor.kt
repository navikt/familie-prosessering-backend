package no.nav.familie.prosessering.sikkerhet

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.rest.TaskControllerNavTokenSupport
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

internal class ProsesseringTaskApiTilgangInterceptor(
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (!erTaskApi(handler) || prosesseringInfoProvider.harTilgang()) {
            return true
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Har ikke tilgang til tasker")
        return false
    }

    private fun erTaskApi(handler: Any): Boolean =
        handler is HandlerMethod && TaskControllerNavTokenSupport::class.java.isAssignableFrom(handler.beanType)
}
