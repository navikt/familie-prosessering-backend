package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.ITaskLogg.Companion.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.util.isOptimisticLocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private const val CRON_DAILY_0700 = "0 0 7 1/1 * ?"
private const val CRON_DAILY_0900 = "0 0 9 1/1 * ?"
private const val CRON_DAILY_1000 = "0 0 10 1/1 * ?"

@Service
class ScheduledTaskService(private val taskService: TaskService, @Value("\${prosessering.delete.after.weeks:2}") private val deleteTasksAfterWeeks: Long) {

    @Scheduled(cron = "\${prosessering.cronRetryTasks:${CRON_DAILY_0700}}")
    @Transactional
    fun retryFeilendeTask() {
        val tasks = taskService.finnAlleFeiledeTasks()
        logger.info("Rekjører ${tasks.size} tasks")

        tasks.forEach {
            try {
                taskService.save(it.klarTilPlukk(BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
            } catch (e: Exception) {
                if(isOptimisticLocking(e)) {
                    logger.info("OptimisticLockingFailureException metode=retryFeilendeTask for task=${it.id}")
                } else {
                    logger.error("Feilet metode=retryFeilendeTask task=${it.id}. Se secure logs")
                    secureLog.info("Feilet metode=retryFeilendeTask task=${it.id}", e)
                }
                return
            }
        }
    }

    @Scheduled(cron = CRON_DAILY_1000)
    @Transactional
    fun settPermanentPlukketTilKlarTilPlukk() {
        val tasks = taskService.finnAllePlukkedeTasks()
        val filtrertTasks = tasks.filter { værtPlukketMinstEnTime(it) }

        logger.info("Fant ${tasks.size} tasks som er plukket. ${filtrertTasks.size} tasks er plukket minst en time siden")

        filtrertTasks
                .forEach {
                    try {
                        taskService.save(it.klarTilPlukk(BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
                    } catch (e: OptimisticLockingFailureException) {
                        if(isOptimisticLocking(e)) {
                            logger.info("OptimisticLockingFailureException metode=settPermanentPlukketTilKlarTilPlukk for task=${it.id}")
                        } else {
                            logger.error("Feilet metode=settPermanentPlukketTilKlarTilPlukk task=${it.id}. Se secure logs")
                            secureLog.info("Feilet metode=settPermanentPlukketTilKlarTilPlukk task=${it.id}", e)
                        }
                        return
                    }
                }
    }

    private fun værtPlukketMinstEnTime(it: ITask): Boolean {
        val sisteLogg = it.logg.maxByOrNull { logg -> logg.opprettetTid }
        return sisteLogg != null
               && sisteLogg.type == Loggtype.PLUKKET
               && sisteLogg.opprettetTid.isBefore(LocalDateTime.now().minusMinutes(60))
    }

    @Scheduled(cron = CRON_DAILY_0900)
    @Transactional
    fun slettTasksKlarForSletting() {
        val klarForSletting = taskService.finnTasksKlarForSletting(LocalDateTime.now().minusWeeks(deleteTasksAfterWeeks))
        logger.info("Sletter ${klarForSletting.size} tasks")
        klarForSletting.forEach {
            logger.info("Task klar for sletting. {} {} {} {}", it.id, it.callId, it.triggerTid, it.status)
            taskService.delete(it)
        }
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(ScheduledTaskService::class.java)
        val secureLog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
