package no.nav.familie.prosessering.internal

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.ITask
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskExecutor
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import kotlin.math.min


@Service
class TaskStepExecutorService(@Value("\${prosessering.maxAntall:10}") private val maxAntall: Int,
                              @Value("\${prosessering.minCapacity:2}") private val minCapacity: Int,
                              @Value("\${prosessering.fixedDelayString.in.milliseconds:30000}")
                              private val fixedDelayString: String,
                              private val taskWorker: TaskWorker,
                              @Qualifier("taskExecutor") private val taskExecutor: TaskExecutor,
                              private val taskRepository: TaskService) {

    private val secureLog = LoggerFactory.getLogger("secureLogger")


    @Scheduled(fixedDelayString = "\${prosessering.fixedDelayString.in.milliseconds:30000}")
    fun pollAndExecute() {
        log.debug("Poller etter nye tasks")
        val pollingSize = calculatePollingSize(maxAntall)

        if (pollingSize > minCapacity) {
            val tasks =
                            taskRepository.finnAlleTasksKlareForProsessering(PageRequest.of(0, pollingSize))
            log.trace("Pollet {} tasks med max {}", tasks.size, maxAntall)

            tasks.forEach(this::executeWork)
        } else {
            log.trace("Pollet ingen tasks siden kapasiteten var {} < {}", pollingSize, minCapacity)
        }
        log.trace("Ferdig med polling, venter {} ms til neste kjøring.", fixedDelayString)
    }

    private fun executeWork(task: ITask) {
        val startTidspunkt = System.currentTimeMillis()
        initLogContext(task)

        val plukketTask = try {
            taskWorker.markerPlukket(task.id) ?: return
        } catch (e: OptimisticLockingFailureException) {
            log.info("OptimisticLockingFailureException for task ${task.id}")
            return
        }

        try {
            taskWorker.doActualWork(plukketTask.id)
            secureLog.info("Fullført kjøring av task '{}', kjøretid={} ms",
                           task,
                           System.currentTimeMillis() - startTidspunkt)
        } catch (e: Exception) {
            e.printStackTrace()
            taskWorker.doFeilhåndtering(task.id, e)
            secureLog.warn("Fullført kjøring av task '{}', kjøretid={} ms, feilmelding='{}'",
                           task,
                           System.currentTimeMillis() - startTidspunkt,
                           e)

        } finally {
            clearLogContext()
        }

    }

    private fun initLogContext(taskDetails: ITask) {
        MDC.put(MDCConstants.MDC_CALL_ID, taskDetails.callId)
        LOG_CONTEXT.add("task", taskDetails.type)
    }

    private fun clearLogContext() {
        LOG_CONTEXT.clear()
        MDC.remove(MDCConstants.MDC_CALL_ID)
    }

    private fun calculatePollingSize(maxAntall: Int): Int {
        val remainingCapacity = (taskExecutor as ThreadPoolTaskExecutor).threadPoolExecutor.queue.remainingCapacity()
        val pollingSize = min(remainingCapacity, maxAntall)
        log.trace("Ledig kapasitet i kø {}, poller etter {}", remainingCapacity, pollingSize)
        return pollingSize
    }


    companion object {

        private val LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess")
        private val log = LoggerFactory.getLogger(TaskStepExecutorService::class.java)
    }
}
