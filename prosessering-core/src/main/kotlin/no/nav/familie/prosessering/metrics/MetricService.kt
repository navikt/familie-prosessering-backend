package no.nav.familie.prosessering.metrics

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
internal class MetricService(
    private val taskMetricRepository: TaskMetricRepository,
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) {
    private val feiledeTasks = MultiGauge.builder("prosessering.tasks.feilet").register(Metrics.globalRegistry)

    @Scheduled(initialDelay = FREKVENS_30_SEC, fixedDelay = FREKVENS_30_MIN)
    fun oppdatertFeiledeTasks() {
        if (prosesseringInfoProvider.isLeader() != false) {
            val rows =
                taskMetricRepository.finnAntallFeiledeTasksPerTypeOgStatus().map {
                    MultiGauge.Row.of(Tags.of(Tag.of("type", it.type), Tag.of("status", it.status.name)), it.count)
                }
            feiledeTasks.register(rows, true)
        }
    }

    companion object {
        const val FREKVENS_30_SEC = 30 * 1000L
        const val FREKVENS_30_MIN = 30 * 60 * 1000L
    }
}
