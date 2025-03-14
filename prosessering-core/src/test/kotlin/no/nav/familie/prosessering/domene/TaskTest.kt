package no.nav.familie.prosessering.domene

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Properties

internal class TaskTest {
    private val tid = LocalDateTime.of(2021, 1, 1, 0, 0, 0)

    @Test
    internal fun `Task skal ikke logge payload eller metadata`() {
        val task =
            Task("Type", "payload", Properties().also { it.put("key", "value") })
                .copy(opprettetTid = tid, triggerTid = tid)
        assertThat(task.toString())
            .isEqualTo("Task(id=0, status=UBEHANDLET, opprettetTid=2021-01-01T00:00, fagsakId=null, behandlingId=null, triggerTid=2021-01-01T00:00, type='Type', versjon=0)")
    }

    @Test
    internal fun `TaskLogg skal ikke logge melding`() {
        val taskLogg =
            TaskLogg(taskId = 1, melding = "melding", type = Loggtype.FEILET)
                .copy(opprettetTid = tid)
        assertThat(taskLogg.toString())
            .isEqualTo("TaskLogg(id=0, type=FEILET, opprettetTid=2021-01-01T00:00)")
    }
}
