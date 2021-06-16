package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.util.isOptimisticLocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private const val CRON_DAILY_0700 = "0 0 7 1/1 * ?"
private const val CRON_DAILY_0900 = "0 0 9 1/1 * ?"
private const val CRON_DAILY_1000 = "0 0 10 1/1 * ?"

@Service
class ScheduledTaskService(private val jobTaskService: JobTaskService) {

    @Scheduled(cron = "\${prosessering.cronRetryTasks:${CRON_DAILY_0700}}")
    fun retryFeilendeTask() {
        try {
            jobTaskService.retryFeilendeTask()
        } catch (e: Exception) {
            if (isOptimisticLocking(e)) {
                loggFeil(e, "retryFeilendeTask")
            }
        }
    }

    @Scheduled(cron = CRON_DAILY_1000)
    fun settPermanentPlukketTilKlarTilPlukk() {
        try {
            jobTaskService.settPermanentPlukketTilKlarTilPlukk()
        } catch (e: Exception) {
            if (isOptimisticLocking(e)) {
                loggFeil(e, "settPermanentPlukketTilKlarTilPlukk")
            }
        }
    }

    @Scheduled(cron = CRON_DAILY_0900)
    fun slettTasksKlarForSletting() {
        try {
            jobTaskService.slettTasksKlarForSletting()
        } catch (e: Exception) {
            loggFeil(e, "slettTasksKlarForSletting")
        }
    }

    private fun loggFeil(e: Exception, metode: String) {
        if (isOptimisticLocking(e)) {
            logger.warn("OptimisticLockingFailureException metode=$metode")
        } else {
            logger.error("Feilet metode=$metode. Se secure logs")
            secureLog.info("Feilet metode=$metode", e)
        }
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(ScheduledTaskService::class.java)
        val secureLog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}