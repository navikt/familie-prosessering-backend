package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.prosessering.AsyncITaskStep
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Status
import org.slf4j.LoggerFactory
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskWorker(private val taskRepository: TaskService, taskStepTyper: List<AsyncITaskStep<out ITask>>) {

    private val taskStepMap: Map<String, AsyncITaskStep<ITask>>

    private val maxAntallFeilMap: Map<String, Int>
    private val triggerTidVedFeilMap: Map<String, Long>
    private val feiltellereForTaskSteps: Map<String, Counter>
    private val settTilManuellOppfølgningVedFeil: Map<String, Boolean>

    init {
        val tasksTilTaskStepBeskrivelse: Map<AsyncITaskStep<ITask>, TaskStepBeskrivelse> =
                taskStepTyper.map { it as AsyncITaskStep<ITask> }.associateWith { task ->
                    val aClass = AopProxyUtils.ultimateTargetClass(task)
                    val annotation = AnnotationUtils.findAnnotation(aClass, TaskStepBeskrivelse::class.java)
                    requireNotNull(annotation) { "annotasjon mangler" }
                    annotation
                }
        taskStepMap = tasksTilTaskStepBeskrivelse.entries.associate { it.value.taskStepType to it.key }
        maxAntallFeilMap = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.maxAntallFeil }
        triggerTidVedFeilMap = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.triggerTidVedFeilISekunder }
        settTilManuellOppfølgningVedFeil = tasksTilTaskStepBeskrivelse.values.associate { it.taskStepType to it.settTilManuellOppfølgning }
        feiltellereForTaskSteps = tasksTilTaskStepBeskrivelse.values.associate {
            it.taskStepType to Metrics.counter("mottak.feilede.tasks",
                                               "status",
                                               it.taskStepType,
                                               "beskrivelse",
                                               it.beskrivelse)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doActualWork(taskId: Long) {

        var task = taskRepository.findById(taskId)

        if (task.status != Status.PLUKKET) {
            return // en annen pod har startet behandling
        }

        task = task.behandler()
        task = taskRepository.save(task)
        // finn tasktype
        val taskStep = finnTaskStep(task.type)

        // execute
        taskStep.preCondition(task)
        taskStep.doTask(task)
        taskStep.postCondition(task)
        taskStep.onCompletion(task)

        task = task.ferdigstill()
        taskRepository.save(task)
        secureLog.trace("Ferdigstiller task='{}'", task)

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doFeilhåndtering(taskId: Long, e: Exception) {
        var task = taskRepository.findById(taskId)
        val maxAntallFeil = finnMaxAntallFeil(task.type)
        val settTilManuellOppfølgning = finnSettTilManuellOppfølgning(task.type)
        secureLog.trace("Behandler task='{}'", task)

        task = task.feilet(TaskFeil(task, e), maxAntallFeil, settTilManuellOppfølgning)
        // lager metrikker på tasks som har feilet max antall ganger.
        if (task.status == Status.FEILET || task.status == Status.MANUELL_OPPFØLGING) {
            finnFeilteller(task.type).increment()
            log.error("Task ${task.id} av type ${task.type} har feilet/satt til manuell oppfølgning. " +
                      "Sjekk familie-prosessering for detaljer")
        }
        task = task.medTriggerTid(task.triggerTid.plusSeconds(finnTriggerTidVedFeil(task.type)))
        taskRepository.save(task)
        secureLog.info("Feilhåndtering lagret ok {}", task)
    }

    private fun finnTriggerTidVedFeil(taskType: String): Long {
        return triggerTidVedFeilMap[taskType] ?: 0
    }

    private fun finnFeilteller(taskType: String): Counter {
        return feiltellereForTaskSteps[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnMaxAntallFeil(taskType: String): Int {
        return maxAntallFeilMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnTaskStep(taskType: String): AsyncITaskStep<ITask> {
        return taskStepMap[taskType] ?: error("Ukjent tasktype $taskType")
    }

    private fun finnSettTilManuellOppfølgning(taskType: String): Boolean {
        return settTilManuellOppfølgningVedFeil[taskType] ?: error("Ukjent tasktype $taskType")
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markerPlukket(id: Long): ITask? {
        var task = taskRepository.findById(id)

        if (task.status.kanPlukkes()) {
            task = task.plukker()
            return taskRepository.save(task)
        }
        return null
    }

    companion object {

        private val secureLog = LoggerFactory.getLogger("secureLogger")
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
