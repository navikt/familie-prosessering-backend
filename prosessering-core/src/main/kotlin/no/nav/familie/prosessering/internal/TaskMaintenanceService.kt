package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.ITaskLogg
import no.nav.familie.prosessering.domene.Loggtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TaskMaintenanceService(private val taskService: TaskService,
                             @Value("\${prosessering.delete.after.weeks:2}") private val deleteTasksAfterWeeks: Long) {

    val antallÅpneTaskGague = MultiGauge.builder("openTasks").register(Metrics.globalRegistry)

    @Transactional
    fun retryFeilendeTask() {
        val tasks = taskService.finnAlleFeiledeTasks()
        logger.info("Rekjører ${tasks.size} tasks")

        tasks.forEach {
            taskService.save(it.klarTilPlukk(ITaskLogg.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
        }
    }

    @Transactional
    fun settPermanentPlukketTilKlarTilPlukk() {
        val tasks = taskService.finnAllePlukkedeTasks()
        val filtrertTasks = tasks.filter { værtPlukketMinstEnTime(it) }

        logger.info("Fant ${tasks.size} tasks som er plukket. ${filtrertTasks.size} tasks er plukket minst en time siden")

        filtrertTasks.forEach {
            taskService.save(it.klarTilPlukk(ITaskLogg.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES))
        }
    }

    private fun værtPlukketMinstEnTime(it: ITask): Boolean {
        val sisteLogg = it.logg.maxByOrNull { logg -> logg.opprettetTid }
        return sisteLogg != null
               && sisteLogg.type == Loggtype.PLUKKET
               && sisteLogg.opprettetTid.isBefore(LocalDateTime.now().minusMinutes(60))
    }

    @Transactional
    fun slettTasksKlarForSletting() {
        val klarForSletting = taskService.finnTasksKlarForSletting(LocalDateTime.now().minusWeeks(deleteTasksAfterWeeks))
        logger.info("Sletter ${klarForSletting.size} tasks")
        klarForSletting.forEach {
            logger.info("Task klar for sletting. {} {} {} {}", it.id, it.callId, it.triggerTid, it.status)
            taskService.delete(it)
        }
    }

    fun tellAntallÅpneTask() {
        val rows = mutableListOf<MultiGauge.Row<Number>>()

        taskService.tellAntallÅpneTasker().map {
            rows.add(
                MultiGauge.Row.of(
                    Tags.of("type", it.type,
                            "status", it.status.name),
                    it.count))
        }

        antallÅpneTaskGague.register(rows, true)
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(TaskScheduler::class.java)
    }

}
