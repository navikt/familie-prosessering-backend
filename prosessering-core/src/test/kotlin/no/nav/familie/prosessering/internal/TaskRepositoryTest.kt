package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.IntegrationRunnerTest
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.AntallÅpneTask
import no.nav.familie.prosessering.domene.Prioritet
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import no.nav.familie.prosessering.util.isOptimisticLocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

class TaskRepositoryTest : IntegrationRunnerTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var repository: TaskRepository

    @Test
    fun `findByPayloadAndType - skal finne task for gitt payload og type`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.FEILET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = UUID.randomUUID().toString(), status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.UBEHANDLET)

        taskService.save(ubehandletTask)
        taskService.save(feiletTask1)
        taskService.save(feiletTask2)

        val funnetTask = repository.findByPayloadAndType(feiletTask1.payload, TaskStep2.TASK_2)

        assertThat(funnetTask?.payload).isEqualTo(feiletTask1.payload)
        assertThat(funnetTask?.type).isEqualTo(feiletTask1.type)
        assertThat(funnetTask?.status).isEqualTo(feiletTask1.status)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker uavhengig av status`() {
        val preCount = repository.findByStatusIn(Status.values().toList(), PageRequest.of(0, 1000))
        val preCountFeilet = preCount.count { it.status == Status.FEILET }
        val preCountUbehandlet = preCount.count { it.status == Status.UBEHANDLET }

        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.FEILET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.UBEHANDLET)

        taskService.save(ubehandletTask)
        taskService.save(feiletTask1)
        taskService.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(Status.values().toList(), PageRequest.of(0, 1000))
        assertThat(alleTasks.size).isEqualTo(3 + preCount.size)
        assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2 + preCountFeilet)
        assertThat(alleTasks.count { it.status == Status.UBEHANDLET }).isEqualTo(1 + preCountUbehandlet)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker gitt en status`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)

        taskService.save(ubehandletTask)
        taskService.save(feiletTask1)
        taskService.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(listOf(Status.FEILET), PageRequest.of(0, 1000))
        assertThat(alleTasks.size).isEqualTo(2)
        assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut max 1 task gitt en status`() {
        val ubehandletTask = Task(type = TaskStep1.TASK_1, payload = "{'a'='b'}", status = Status.UBEHANDLET)
        val feiletTask1 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)
        val feiletTask2 = Task(type = TaskStep2.TASK_2, payload = "{'a'='1'}", status = Status.FEILET)

        taskService.save(ubehandletTask)
        taskService.save(feiletTask1)
        taskService.save(feiletTask2)

        val alleTasks = repository.findByStatusIn(listOf(Status.FEILET), PageRequest.of(0, 1))
        assertThat(alleTasks.size).isEqualTo(1)
        assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(1)
    }

    @Test
    fun `findByStatus - skall returnere tasks sortert etter opprettet_tid`() {
        val task1 = Task(type = TaskStep1.TASK_1, payload = "1", opprettetTid = LocalDateTime.now())
        val task2 = Task(type = TaskStep2.TASK_2, payload = "2", opprettetTid = LocalDateTime.now().minusDays(1))
        val task3 = Task(type = TaskStep2.TASK_2, payload = "3", opprettetTid = LocalDateTime.now().plusDays(1))

        taskService.save(task1)
        taskService.save(task2)
        taskService.save(task3)

        val message = repository.findByStatusIn(
            listOf(Status.UBEHANDLET),
            PageRequest.of(0, 3, Sort.Direction.DESC, "opprettetTid"),
        )
        assertThat(message.map { it.payload }).containsExactly("3", "1", "2")
    }

    @Test
    fun `findByStatusInAndTriggerTidBeforeOrderByPrioritetDescOpprettetTidAsc - skal returnere tasks sortert etter prioritet, og sen opprettet_tid`() {
        val opprettetTid = LocalDateTime.now()
        val taskNå = Task(
            type = TaskStep1.TASK_1,
            payload = "nå, prioritet 0",
            opprettetTid = opprettetTid,
            prioritet = Prioritet.NORMAL,
        )
        val taskNåprioritet1 = taskNå.copy(payload = "nå, prioritet 1", prioritet = Prioritet.VIKTIG)
        val task2 = Task(
            type = TaskStep1.TASK_1,
            payload = "1 dag siden, prioritet 0",
            opprettetTid = opprettetTid.minusDays(1),
            prioritet = Prioritet.NORMAL,
        )
        val task2Prioritet1 = task2.copy(payload = "1 dag siden, prioritet 1", prioritet = Prioritet.VIKTIG)
        val task3 = Task(
            type = TaskStep1.TASK_1,
            payload = "neste dag, prioritet 0",
            opprettetTid = LocalDateTime.now().plusDays(1),
            prioritet = Prioritet.NORMAL,
        )
        val task3Prioritet1 = task3.copy(payload = "neste dag, prioritet 1", prioritet = Prioritet.VIKTIG)

        taskService.save(taskNå)
        taskService.save(task3Prioritet1)
        taskService.save(task2)
        taskService.save(taskNåprioritet1)
        taskService.save(task2Prioritet1)
        taskService.save(task3)

        val tasks = repository.findByStatusInAndTriggerTidBeforeOrderByPrioritetDescOpprettetTidAsc(
            listOf(Status.UBEHANDLET),
            LocalDateTime.now(),
            PageRequest.of(0, 10),
        ).map { it.payload }

        assertThat(tasks).containsExactly(
            "1 dag siden, prioritet 1",
            "nå, prioritet 1",
            "neste dag, prioritet 1",
            "1 dag siden, prioritet 0",
            "nå, prioritet 0",
            "neste dag, prioritet 0",
        )
    }

    @Test
    internal fun `skal sorterte tasks etter prioritet`() {
        val opprettetTid = LocalDateTime.now()

        taskService.saveAll(listOf(
            Prioritet.NORMAL,
            Prioritet.KRITISK,
            Prioritet.LITE_VIKTIG,
            Prioritet.VIKTIG,
            Prioritet.IKKE_VIKTIG
        ).map {
            Task(
                type = TaskStep1.TASK_1,
                payload = UUID.randomUUID().toString(),
                opprettetTid = opprettetTid,
                prioritet = it,
            )
        })

        val tasks = repository.findByStatusInAndTriggerTidBeforeOrderByPrioritetDescOpprettetTidAsc(
            listOf(Status.UBEHANDLET),
            LocalDateTime.now(),
            PageRequest.of(0, 10),
        ).map { it.prioritet }

        assertThat(tasks).containsExactly(
            Prioritet.KRITISK,
            Prioritet.VIKTIG,
            Prioritet.NORMAL,
            Prioritet.LITE_VIKTIG,
            Prioritet.IKKE_VIKTIG
        )
    }

    @Test
    fun `findByStatusInAndTriggerTidBeforeOrderByOpprettetTid - skal returnere tasks sortert etter opprettet_tid - eldst først`() {
        val task1 =
            Task(type = TaskStep1.TASK_1, payload = "task med opprettetTid nå", opprettetTid = LocalDateTime.now())
        val task2 = Task(
            type = TaskStep2.TASK_2,
            payload = "task med tidligst oppprettetTid",
            opprettetTid = LocalDateTime.now().minusDays(1),
        )
        val task3 =
            Task(
                type = TaskStep2.TASK_2,
                payload = "task med senest oppettetTid",
                opprettetTid = LocalDateTime.now().plusDays(1),
            )

        taskService.save(task1)
        taskService.save(task2)
        taskService.save(task3)

        val message = repository.findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(
            listOf(Status.UBEHANDLET),
            LocalDateTime.now(),
            PageRequest.of(0, 5),
        )

        assertThat(message.map { it.payload }).containsExactly(
            "task med tidligst oppprettetTid",
            "task med opprettetTid nå",
            "task med senest oppettetTid",
        )
    }

    @Test
    fun `countOpenTask - skal returenere antall åpne tasker`() {
        val task1 = Task(type = TaskStep1.TASK_1, payload = "1", opprettetTid = LocalDateTime.now())
        val task2 = Task(type = TaskStep2.TASK_2, payload = "1", opprettetTid = LocalDateTime.now())
        val task3 = Task(type = TaskStep2.TASK_2, payload = "1", opprettetTid = LocalDateTime.now())

        taskService.save(task1)
        taskService.save(task2)
        taskService.save(task3)

        val åpneTask = repository.countOpenTasks()
        assertThat(åpneTask).hasSize(2).contains(
            AntallÅpneTask(TaskStep1.TASK_1, Status.UBEHANDLET, 1),
            AntallÅpneTask(TaskStep2.TASK_2, Status.UBEHANDLET, 2),
        )
    }

    @Test
    fun `skal håndtere properties`() {
        val property = "PROPERTY"
        val lagretTask = taskService.save(
            Task(
                type = TaskStep1.TASK_1,
                payload = "{'a'='b'}",
                properties = Properties().apply {
                    this[property] = property
                },
            ),
        )
        val task = repository.findByIdOrNull(lagretTask.id)!!
        assertThat(task.metadata.getProperty(property)).isEqualTo(property)
    }

    @Test
    internal fun `skal håndtere optimistic locking`() {
        val task = taskService.save(Task(TaskStep1.TASK_1, "{'a'='b'}"))
        taskService.save(task.copy(status = Status.KLAR_TIL_PLUKK))
        assertThat(catchThrowable { taskService.save(task.copy(status = Status.KLAR_TIL_PLUKK)) })
            .matches { isOptimisticLocking(it as Exception) }
    }

    @Test
    internal fun `finnTasksSomErFerdigNåMenFeiletFør skal finne tasks som feilet`() {
        var task = taskService.save(Task(TaskStep1.TASK_1, "{'a':'b'}").copy(status = Status.FERDIG))

        assertThat(repository.finnTasksSomErFerdigNåMenFeiletFør(Pageable.unpaged())).isEmpty()

        task = taskService.feilet(task, TaskFeil(task, null), 0, 1, false)
        assertThat(repository.finnTasksSomErFerdigNåMenFeiletFør(Pageable.unpaged())).isEmpty()

        taskService.ferdigstill(task)
        val tasks = repository.finnTasksSomErFerdigNåMenFeiletFør(Pageable.unpaged())
        assertThat(tasks).hasSize(1)
    }
}
