package no.nav.familie.prosessering.internal

import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.domene.AntallÅpneTask
import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.familie.prosessering.domene.TaskLoggMetadata
import no.nav.familie.prosessering.domene.TaskLoggRepository
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.LocalDateTime

@Component
class TaskService internal constructor(
    private val taskRepository: TaskRepository,
    private val taskLoggRepository: TaskLoggRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLog = LoggerFactory.getLogger("secureLogger")

    fun findById(id: Long): Task {
        return taskRepository.findByIdOrNull(id) ?: error("Task med id: $id ikke funnet.")
    }

    /**
     * Brukes for å opprette task
     */
    @Suppress("unused") // brukes av klienter
    @Transactional
    fun save(task: Task): Task {
        val lagretTask = taskRepository.save(task)
        validerTask(lagretTask)
        if (task.versjon == 0L) {
            taskLoggRepository.save(TaskLogg(type = Loggtype.UBEHANDLET, taskId = lagretTask.id))
        }
        return lagretTask
    }

    /**
     * Brukes for å opprette flere tasks
     */
    @Suppress("unused") // brukes av klienter
    @Transactional
    fun saveAll(tasks: Collection<Task>): List<Task> {
        return tasks.map { save(it) }
    }

    private fun validerTask(task: Task) {
        if ((task.versjon == 0L && task.id != 0L) || (task.id == 0L && task.versjon != 0L)) {
            error("Når man oppretter en ny task må task og versjon være satte til 0")
        }
    }

    fun finnAlleTasksKlareForProsessering(page: Pageable): List<Task> {
        return taskRepository.findByStatusInAndTriggerTidBeforeOrderByOpprettetTid(
            listOf(
                Status.KLAR_TIL_PLUKK,
                Status.UBEHANDLET,
            ),
            LocalDateTime.now(),
            page,
        )
    }

    fun finnAlleFeiledeTasks(): List<Task> {
        return taskRepository.findByStatus(Status.FEILET)
    }

    internal fun finnAllePlukkedeTasks(tid: LocalDateTime): Pair<Long, List<Task>> {
        return taskRepository.countByStatusIn(listOf(Status.PLUKKET)) to
            taskRepository.findAllByStatusAndLastProcessed(Status.PLUKKET, tid)
    }

    @Deprecated("Bruk finnTasksMedStatus", ReplaceWith("finnTasksMedStatus(status, type, page)"))
    fun finnTasksMedStatus(status: List<Status>, page: Pageable): List<Task> {
        return taskRepository.findByStatusIn(status, page)
    }

    fun finnTasksMedStatus(status: List<Status>, type: String? = null, page: Pageable = Pageable.unpaged()): List<Task> {
        return if (type == null) {
            taskRepository.findByStatusIn(status, page)
        } else {
            taskRepository.findByStatusInAndType(status, type, page)
        }
    }

    @Transactional
    internal fun slettTasks(eldreEnnDato: LocalDateTime, antall: Int): Int {
        val taskIds = taskRepository.finnTasksTilSletting(eldreEnnDato, antall)
        if (taskIds.isEmpty()) return 0

        taskLoggRepository.deleteAllByTaskIdIn(taskIds)
        taskRepository.deleteAllById(taskIds)
        return taskIds.size
    }

    fun finnTasksSomErFerdigNåMenFeiletFør(page: Pageable): List<Task> =
        taskRepository.finnTasksSomErFerdigNåMenFeiletFør(page)

    fun finnAlleTasksMedCallId(callId: String): List<Task> =
        taskRepository.finnTaskerMedCallId(callId)

    internal fun finnTaskLoggMetadata(taskIds: List<Long>): Map<Long, TaskLoggMetadata> {
        if (taskIds.isEmpty()) return emptyMap()
        return taskLoggRepository.finnTaskLoggMetadata(taskIds).associateBy { it.taskId }
    }

    fun findTaskLoggByTaskId(taskId: Long): List<TaskLogg> {
        return taskLoggRepository.findByTaskId(taskId)
    }

    @Suppress("unused") // brukes av klienter
    fun finnTaskMedPayloadOgType(payload: String, type: String): Task? {
        return taskRepository.findByPayloadAndType(payload, type)
    }

    /**
     * Då taskRepository er internal så kan denne fortsatt være fin å bruke fra tests
     */
    @Suppress("unused") // brukes av klienter
    fun findAll(): List<Task> {
        return taskRepository.findAll().toList()
    }

    @Transactional
    fun delete(task: Task) {
        taskLoggRepository.deleteAllByTaskId(task.id)
        taskRepository.delete(task)
    }

    @Transactional
    @Suppress("unused") // brukes av klienter
    fun deleteAll(tasks: Collection<Task>) {
        if (tasks.toList().isEmpty()) return
        taskLoggRepository.deleteAllByTaskIdIn(tasks.map { it.id })
        taskRepository.deleteAll(tasks)
    }

    internal fun antallTaskerTilOppfølging(): Long {
        return taskRepository.countByStatusIn(listOf(Status.MANUELL_OPPFØLGING, Status.FEILET))
    }

    internal fun tellAntallÅpneTasker(): List<AntallÅpneTask> {
        return taskRepository.countOpenTasks()
    }

    fun antallFeil(taskId: Long): Int {
        return taskLoggRepository.countByTaskIdAndType(taskId, Loggtype.FEILET)
    }

    @Transactional
    internal fun avvikshåndter(task: Task, avvikstype: Avvikstype, årsak: String, endretAv: String): Task {
        val taskLogg = TaskLogg(
            taskId = task.id,
            type = Loggtype.AVVIKSHÅNDTERT,
            melding = årsak,
            endretAv = endretAv,
        )
        taskLoggRepository.save(taskLogg)
        return taskRepository.save(task.copy(status = Status.AVVIKSHÅNDTERT, avvikstype = avvikstype))
    }

    @Transactional
    internal fun kommenter(task: Task, kommentar: String, endretAv: String, settTilManuellOppfølgning: Boolean): Task {
        val taskLogg =
            TaskLogg(taskId = task.id, type = Loggtype.KOMMENTAR, melding = kommentar, endretAv = endretAv)
        taskLoggRepository.save(taskLogg)
        return taskRepository.save(task.copy(status = if (settTilManuellOppfølgning) Status.MANUELL_OPPFØLGING else task.status))
    }

    @Transactional
    internal fun behandler(task: Task): Task {
        taskLoggRepository.save(TaskLogg(taskId = task.id, type = Loggtype.BEHANDLER))
        return taskRepository.save(task.copy(status = Status.BEHANDLER))
    }

    @Transactional
    internal fun klarTilPlukk(task: Task, endretAv: String, melding: String? = null): Task {
        val taskLogg =
            TaskLogg(
                taskId = task.id,
                type = Loggtype.KLAR_TIL_PLUKK,
                endretAv = endretAv,
                melding = melding,
            )
        taskLoggRepository.save(taskLogg)
        return taskRepository.save(task.copy(status = Status.KLAR_TIL_PLUKK))
    }

    @Transactional
    internal fun plukker(task: Task): Task {
        taskLoggRepository.save(TaskLogg(taskId = task.id, type = Loggtype.PLUKKET))
        return taskRepository.save(task.copy(status = Status.PLUKKET))
    }

    @Transactional
    internal fun ferdigstill(task: Task): Task {
        taskLoggRepository.save(TaskLogg(taskId = task.id, type = Loggtype.FERDIG))
        return taskRepository.save(task.copy(status = Status.FERDIG))
    }

    @Transactional
    internal fun feilet(
        task: Task,
        feil: TaskFeil,
        tidligereAntallFeil: Int,
        maxAntallFeil: Int,
        settTilManuellOppfølgning: Boolean,
    ): Task {
        val nyStatus = nyFeiletStatus(tidligereAntallFeil, maxAntallFeil, settTilManuellOppfølgning)

        val feilmelding = try {
            feil.writeValueAsString()
        } catch (e: IOException) {
            logger.warn("Feilet lagring av task=${task.id} med melding. Se secure logs")
            secureLog.warn("Feilet lagring av task=${task.id} med melding", e)
            "Feilet skriving av feil til json exceptionCauseMessage=${feil.exceptionCauseMessage} feilmelding=${feil.feilmelding} stacktrace=${feil.stackTrace}"
        }
        taskLoggRepository.save(TaskLogg(taskId = task.id, type = Loggtype.FEILET, melding = feilmelding))
        return taskRepository.save(task.copy(status = nyStatus))
    }

    private fun nyFeiletStatus(
        tidligereAntallFeil: Int,
        maxAntallFeil: Int,
        settTilManuellOppfølgning: Boolean,
    ): Status {
        val antallFeilendeForsøk = tidligereAntallFeil + 1
        return when {
            maxAntallFeil > antallFeilendeForsøk -> Status.KLAR_TIL_PLUKK
            settTilManuellOppfølgning -> Status.MANUELL_OPPFØLGING
            else -> Status.FEILET
        }
    }
}
