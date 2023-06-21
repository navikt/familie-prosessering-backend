package no.nav.familie.prosessering.internal

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.prosessering.domene.TaskLogg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Service
class TaskMaintenanceService(
    private val taskService: TaskService,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${prosessering.delete.after.weeks:2}") private val deleteTasksAfterWeeks: Long,
    @Value("\${prosessering.delete.pagesize:10000}") private val deleteTasksPageSize: Int,
) {

    val antallÅpneTaskGague = MultiGauge.builder("openTasks").register(Metrics.globalRegistry)

    @Transactional
    fun retryFeilendeTask() {
        val tasks = taskService.finnAlleFeiledeTasks()
        logger.info("Rekjører ${tasks.size} tasks")

        tasks.forEach {
            taskService.klarTilPlukk(it, TaskLogg.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES)
        }
    }

    @Transactional
    fun settPermanentPlukketTilKlarTilPlukk() {
        val enTimeSiden = LocalDateTime.now().minusHours(1)
        val (antallPlukkende, tasks) = taskService.finnAllePlukkedeTasks(enTimeSiden)

        logger.info("Fant $antallPlukkende tasks som er plukket. ${tasks.size} tasks er plukket minst en time siden")

        tasks.forEach {
            taskService.klarTilPlukk(it, TaskLogg.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES)
        }
    }

    fun slettTasksKlarForSletting() {
        val eldreEnnDato = LocalDateTime.now().minusWeeks(deleteTasksAfterWeeks)
        while (true) {
            val antallTasksSomSlettes = taskService.slettTasks(eldreEnnDato, deleteTasksPageSize)
            logger.info("Slettet $antallTasksSomSlettes tasks som har triggerTid før $eldreEnnDato")
            if (antallTasksSomSlettes == 0) {
                return
            }
        }
    }

    fun tellAntallÅpneTask() {
        val rows = mutableListOf<MultiGauge.Row<Number>>()

        taskService.tellAntallÅpneTasker().map {
            rows.add(
                MultiGauge.Row.of(
                    Tags.of(
                        "type",
                        it.type,
                        "status",
                        it.status.name,
                    ),
                    it.count,
                ),
            )
        }

        antallÅpneTaskGague.register(rows, true)
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(TaskScheduler::class.java)
    }
}
