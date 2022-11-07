package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.TaskStepBeskrivelse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils

class AsyncTaskStepTest : IntegrationRunnerTest() {

    @Autowired
    private lateinit var tasker: List<AsyncTaskStep>

    @Test
    fun `skal ha annotasjon`() {
        Assertions.assertThat(
            tasker.any {
                harIkkePåkrevdAnnotasjon(it)
            }
        ).isFalse()
    }

    private fun harIkkePåkrevdAnnotasjon(it: AsyncTaskStep): Boolean {
        return !AnnotationUtils.isAnnotationDeclaredLocally(
            TaskStepBeskrivelse::class.java,
            it.javaClass
        )
    }

    @Test
    fun `skal ha unike navn`() {
        val taskTyper = tasker.map { taskStep: AsyncTaskStep -> finnAnnotasjon(taskStep).taskStepType }

        Assertions.assertThat(taskTyper)
            .isEqualTo(taskTyper.distinct())
    }

    private fun finnAnnotasjon(taskStep: AsyncTaskStep): TaskStepBeskrivelse {
        val aClass = AopProxyUtils.ultimateTargetClass(taskStep)
        return AnnotationUtils.findAnnotation(aClass, TaskStepBeskrivelse::class.java) ?: error("")
    }
}
