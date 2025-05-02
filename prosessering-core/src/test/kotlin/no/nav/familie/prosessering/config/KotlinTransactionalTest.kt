package no.nav.familie.prosessering.config

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLoggRepository
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.io.IOException

class KotlinTransactionalTest : IntegrationRunnerTest() {
    @Autowired
    private lateinit var service: KotlinTransactionalTestService

    @AfterEach
    fun tearDown() {
        service.slettAlle()
        super.clear()
    }

    @Test
    fun `happy case`() {
        service.lagre()

        assertThat(service.antall()).isEqualTo(1)
    }

    @Test
    fun `skal rulle tilbake når checked exception IOException kastes`() {
        val exception =
            try {
                service.lagreMedFeil()
            } catch (e: Exception) {
                e
            }

        assertThat(service.antall()).isEqualTo(0)
        assertThat(exception).isInstanceOf(IOException::class.java)
    }
}

@Service
internal class KotlinTransactionalTestService(
    private val taskRepository: TaskRepository,
    private val taskLoggRepository: TaskLoggRepository,
) {
    @KotlinTransactional(propagation = Propagation.REQUIRES_NEW)
    fun lagre() {
        taskRepository.save(Task(TaskStep1.TASK_1, "1"))
    }

    @KotlinTransactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreMedFeil() {
        taskRepository.save(Task(TaskStep1.TASK_1, "1"))
        throw IOException("Simulert feil")
    }

    @KotlinTransactional(propagation = Propagation.REQUIRES_NEW)
    fun antall(): Long = taskRepository.count()

    @KotlinTransactional(propagation = Propagation.REQUIRES_NEW)
    fun slettAlle() {
        taskLoggRepository.deleteAll()
        taskRepository.deleteAll()
    }
}
