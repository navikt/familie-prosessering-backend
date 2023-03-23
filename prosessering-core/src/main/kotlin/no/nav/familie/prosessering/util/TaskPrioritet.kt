package no.nav.familie.prosessering.util

import org.slf4j.MDC

internal object TaskPrioritet {
    const val MDC_TASK_PRIORITET = "taskPrio"

    fun gjenbrukTaskPrioritetEller0(): Int {
        return MDC.get(MDC_TASK_PRIORITET)?.toIntOrNull() ?: 0
    }
}
