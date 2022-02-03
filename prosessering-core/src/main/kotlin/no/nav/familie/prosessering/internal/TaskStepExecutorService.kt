package no.nav.familie.prosessering.internal

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.util.isOptimisticLocking
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.min

/**
 * @param [continuousRunningEnabled] hvis true så kjører man tasks direkt på nytt etter att man behandlet tasks
 */
@Service
class TaskStepExecutorService(@Value("\${prosessering.maxAntall:10}") private val maxAntall: Int,
                              @Value("\${prosessering.continuousRunning.enabled:false}")
                              private val continuousRunningEnabled: Boolean,
                              @Value("\${prosessering.enabled:true}")
                              private val enabled: Boolean,
                              @Value("\${prosessering.fixedDelayString.in.milliseconds:1000}")
                              private val fixedDelayString: String,
                              private val taskWorker: TaskWorker,
                              private val threadPoolTaskScheduler: ThreadPoolTaskScheduler,
                              @Qualifier("taskExecutor") private val taskExecutor: TaskExecutor,
                              private val taskService: TaskService) : ApplicationListener<ContextClosedEvent> {

    private val secureLog = LoggerFactory.getLogger("secureLogger")

    init {
        log.info("Prosessering enabled=$enabled")
    }

    /**
     * Denne settes til true når appen fått SIGTERM
     *
     * Pga [ThreadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown] som settes til true for å håndtere att tasker
     * har mulighet til å kjøre klart, så er også fortsatt mulig å legge til flere tasks, som vi ikke ønsker
     */
    @Volatile private var isShuttingDown = false

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }

    @Scheduled(fixedDelayString = "\${prosessering.fixedDelayString.in.milliseconds:30000}")
    fun pollAndExecute() {
        if (!enabled) return

        while (true) {
            if (isShuttingDown) {
                log.info("Shutting down, does not start new pollAndExecuteTasks")
                return
            }
            if (!pollAndExecuteTasks()) return
        }
    }

    /**
     * @return if it should continue to run or not
     */
    private fun pollAndExecuteTasks(): Boolean {
        log.debug("Poller etter nye tasks")
        val pollingSize = calculatePollingSize(maxAntall)

        if (pollingSize != 0) {
            val tasks = taskService.finnAlleTasksKlareForProsessering(PageRequest.of(0, pollingSize))
            log.trace("Pollet {} tasks med max {}", tasks.size, maxAntall)

            if (tasks.isNotEmpty()) {
                return executeTasks(tasks)
            }
        } else {
            log.trace("Ingen tasks til prosessering.")
        }
        log.trace("Ferdig med polling, venter {} ms til neste kjøring.", fixedDelayString)
        return false
    }

    private fun executeTasks(tasks: List<ITask>): Boolean {
        if (!continuousRunningEnabled) {
            tasks.forEach { taskExecutor.execute { executeWork(it) } }
            return false
        }
        val futures = tasks.map { task ->
            CompletableFuture.runAsync({ executeWork(task) }, taskExecutor)
        }
        try {
            CompletableFuture.allOf(*futures.toTypedArray()).get(2, TimeUnit.MINUTES)
        } catch (e: TimeoutException) {
            log.warn("En av taskene av ${tasks.map(ITask::id)} klarte ikke å fullføre innen timeout")
        }
        return true
    }

    private fun executeWork(task: ITask) {
        val startTidspunkt = System.currentTimeMillis()
        initLogContext(task)

        val plukketTask = try {
            taskWorker.markerPlukket(task.id) ?: return
        } catch (e: Exception) {
            if (isOptimisticLocking(e)) {
                log.info("OptimisticLockingFailureException metode=executeWork for task=${task.id}")
            } else {
                log.error("Feilet metode=executeWork task=${task.id}. Se secure logs")
                secureLog.info("Feilet metode=executeWork task=${task.id}", e)
            }
            return
        }

        try {
            taskWorker.doActualWork(plukketTask.id)
            secureLog.info("Fullført kjøring av task '{}', kjøretid={} ms",
                           task,
                           System.currentTimeMillis() - startTidspunkt)
        } catch (e: RekjørSenereException) {
            taskWorker.rekjørSenere(task.id, e)
        } catch (e: Exception) {
            secureLog.info("Feilhåndterer task=${task.id} message=${e.message}")
            taskWorker.doFeilhåndtering(task.id, e)
            secureLog.warn("Fullført kjøring av task '{}', kjøretid={} ms med feil",
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
