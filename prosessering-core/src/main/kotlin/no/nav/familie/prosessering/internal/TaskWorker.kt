package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Status
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskWorker(private val taskRepository: TaskService, private val taskStepService: TaskStepService) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun doActualWork(taskId: Long) {

        var task = taskRepository.findById(taskId)

        if (task.status != Status.PLUKKET) {
            return // en annen pod har startet behandling
        }

        task = task.behandler()
        task = taskRepository.save(task)
        // finn tasktype
        val taskStep = taskStepService.finnTaskStep(task.type)

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
        val maxAntallFeil = taskStepService.finnMaxAntallFeil(task.type)
        secureLog.trace("Behandler task='{}'", task)

        task = task.feilet(TaskFeil(task, e), maxAntallFeil)
        // lager metrikker på tasks som har feilet max antall ganger.
        if (task.status == Status.FEILET) {
            taskStepService.finnFeilteller(task.type).increment()
            log.error("Task ${task.id} av type ${task.type} har feilet. " +
                      "Sjekk familie-prosessering for detaljer")
        }
        task = task.medTriggerTid(task.triggerTid.plusSeconds(taskStepService.finnTriggerTidVedFeil(task.type)))
        taskRepository.save(task)
        secureLog.info("Feilhåndtering lagret ok {}", task)
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
