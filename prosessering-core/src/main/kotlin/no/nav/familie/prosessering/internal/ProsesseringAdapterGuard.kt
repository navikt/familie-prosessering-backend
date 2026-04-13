package no.nav.familie.prosessering.internal

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

@Component
internal class ProsesseringAdapterGuard {
    @PostConstruct
    fun verifiserKunEttWebAdapter() {
        val springSecurityErTilstede =
            ClassUtils.isPresent(
                "no.nav.familie.prosessering.rest.TaskControllerSpringSecurity",
                javaClass.classLoader,
            )

        val navTokenSuppertErTilstede =
            ClassUtils.isPresent(
                "no.nav.familie.prosessering.rest.TaskControllerNavTokenSupport",
                javaClass.classLoader,
            )

        check(!(springSecurityErTilstede && navTokenSuppertErTilstede)) {
            "Både prosessering-web-spring-security og prosessering-web-nav-token-support er i classpathen. " +
                    "Velg nøyaktig én adapter-dependency."
        }
    }
}
