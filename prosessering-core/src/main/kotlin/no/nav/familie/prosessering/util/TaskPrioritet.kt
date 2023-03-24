package no.nav.familie.prosessering.util

import no.nav.familie.prosessering.domene.Prioritet
import org.slf4j.MDC

internal object TaskPrioritet {
    const val MDC_TASK_PRIORITET = "taskPrio"

    fun gjenbrukTaskPrioritetEllerBrukNormal(): Prioritet {
        return MDC.get(MDC_TASK_PRIORITET)?.let { Prioritet.valueOf(it) } ?: Prioritet.NORMAL
    }
}
