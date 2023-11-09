package no.nav.familie.prosessering.internal

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.transaction.TestTransaction

class TaskServiceIntegrationTest : IntegrationRunnerTest() {

    private lateinit var loggingEvents: List<ILoggingEvent>

    @Autowired
    lateinit var taskService: TaskService

    private val task = Task(TaskStep2.TASK_2, "{'a'='b'}")

    @BeforeEach
    fun setUp() {
        val listAppender = ListAppender<ILoggingEvent>()
        (LoggerFactory.getLogger(TaskTransactionSynchronization::class.java) as Logger).addAppender(listAppender)
        listAppender.start()
        loggingEvents = listAppender.list
    }

    @Test
    fun `save skal ikke kjøre task direkte`() {
        val task = taskService.save(task)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        Thread.sleep(2000)

        assertThat(taskService.findById(task.id).status).isEqualTo(Status.UBEHANDLET)
        assertThat(loggingEvents.filter { it.message == "Kaller på pollAndExecute" }).isEmpty()
    }

    @Test
    fun `saveAndPoll skal polle etter nye tasks direkte`() {
        val task = taskService.saveAndPoll(task)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        Thread.sleep(2000)

        assertThat(taskService.findById(task.id).status).isEqualTo(Status.FERDIG)
        assertThat(loggingEvents.filter { it.message == "Kaller på pollAndExecute" }).hasSize(1)
    }

    @Test
    fun `saveAndPoll skal polle etter tasks direkte, og kun en gang per tråd`() {
        taskService.saveAndPoll(task)
        taskService.saveAndPoll(task)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        Thread.sleep(500)

        assertThat(taskService.findAll().map { it.status }).containsExactly(Status.FERDIG, Status.FERDIG)
        assertThat(loggingEvents.filter { it.message == "Kaller på pollAndExecute" }).hasSize(1)
    }
}
