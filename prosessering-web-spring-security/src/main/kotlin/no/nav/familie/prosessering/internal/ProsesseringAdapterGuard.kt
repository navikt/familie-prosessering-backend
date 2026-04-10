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
                "no.nav.familie.prosessering.rest.TaskControllerNavTokenSupport",
                javaClass.classLoader,
            )

        check(!otherPresent) {
            "Both prosessering-web-spring-security and prosessering-web-nav-token-support are on the classpath. " +
                "Choose exactly one adapter dependency."
        }
    }
}
