package no.nav.familie.prosessering.internal

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

@Component
internal class ProsesseringAdapterGuard {
    @PostConstruct
    fun verifySingleAdapterPresent() {
        val otherPresent =
            ClassUtils.isPresent(
                "no.nav.familie.prosessering.rest.TaskControllerSpringSecurity",
                javaClass.classLoader,
            )

        check(!otherPresent) {
            "Both prosessering-web-nav-token-support and prosessering-web-spring-security are on the classpath. " +
                "Choose exactly one adapter dependency."
        }
    }
}
