package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service

@Service
class TaskStepService(taskStepTyper: List<AsyncTaskStep>) {

    private val taskStepMap: Map<String, AsyncTaskStep>

    private val maxAntallFeilMap: Map<String, Int>
    private val triggerTidVedFeilMap: Map<String, Long>
    private val feiltellereForTaskSteps: Map<String, Counter>
    private val settTilManuellOppfølgningVedFeil: Map<String, Boolean>

    init {
        val tasksTilTaskStepBeskrivelse: Map<AsyncTaskStep, TaskStepBeskrivelse> = taskStepTyper.associateWith { task ->
            val aClass = AopProxyUtils.ultimateTargetClass(task)
            val annotation = AnnotationUtils.findAnnotation(aClass, TaskStepBeskrivelse::class.java)
            requireNotNull(annotation) { "annotasjon mangler" }
            annotation
        }
        taskStepMap = tasksTilTaskStepBeskrivelse.entries.associate { it.value.taskStepType to it.key }
        maxAntallFeilMap = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.maxAntallFeil }
        triggerTidVedFeilMap = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.triggerTidVedFeilISekunder }
        feiltellereForTaskSteps = tasksTilTaskStepBeskrivelse.values.associate {
            it.taskStepType to Metrics.counter("mottak.feilede.tasks",
                                               "status",
                                               it.taskStepType,
                                               "beskrivelse",
                                               it.beskrivelse)
        }
        settTilManuellOppfølgningVedFeil = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.settTilManuellOppfølgning }
    }

    internal fun finnFeilteller(taskType: String): Counter {
        return feiltellereForTaskSteps[taskType] ?: error("Ukjent tasktype $taskType")
    }

    fun finnTriggerTidVedFeil(taskType: String): Long {
        return triggerTidVedFeilMap[taskType] ?: 0
    }

    fun finnMaxAntallFeil(taskType: String): Int {
        return maxAntallFeilMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    fun finnTaskStep(taskType: String): AsyncTaskStep {
        return taskStepMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    fun finnSettTilManuellOppfølgning(taskType: String): Boolean {
        return settTilManuellOppfølgningVedFeil[taskType] ?: error("Ukjent tasktype $taskType")
    }

}
