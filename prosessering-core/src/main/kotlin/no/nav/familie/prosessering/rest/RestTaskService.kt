package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskerMedStatusFeiletOgManuellOppfølging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RestTaskService(private val taskService: TaskService) {

    fun finnAntallTaskerSomKreverOppfølging(): Ressurs<Long> {
        return Result.runCatching {
            taskService.antallTaskerTilOppfølging()
        }.fold(
            onSuccess = { Ressurs.success(it) },
            onFailure = { e ->
                logger.error("Henting av antall tasker som krever oppfølging feilet", e)
                Ressurs.failure(errorMessage = "Henting av antall tasker som krever oppfølging feilet.", error = e)
            },
        )
    }

    fun finnAntallTaskerMedStatusFeiletOgManuellOppfølging():
            Ressurs<TaskerMedStatusFeiletOgManuellOppfølging> {
        val errorMelding = "Henting av antall tasker som har feilet eller er som satt til manuell oppføling feilet."
        return Result.runCatching {
            taskService.antallTaskerMedStatusFeiletOgManuellOppfølging()
        }.fold(
            onSuccess = { Ressurs.success(it) },
            onFailure = { e ->
                logger.error(errorMelding, e)
                Ressurs.failure(errorMessage = errorMelding, error = e)
            },
        )
    }

    fun hentTasksSomErFerdigNåMenFeiletFør(brukernavn: String): Ressurs<PaginableResponse<TaskDto>>? {
        logger.info("$brukernavn henter oppgaver som er ferdige nå, men feilet før")
        return hentTasksGittSpørring(0) { pageRequest: PageRequest ->
            taskService.finnTasksSomErFerdigNåMenFeiletFør(pageRequest)
        }
            .fold(
                onSuccess = { Ressurs.success(data = it) },
                onFailure = { e ->
                    logger.error("Henting av tasker feilet", e)
                    Ressurs.failure(
                        errorMessage = "Henter oppgaver som er ferdige nå, men feilet før feilet.",
                        error = e,
                    )
                },
            )
    }

    fun hentTasks(
        statuses: List<Status>,
        saksbehandlerId: String,
        page: Int,
        type: String?,
    ): Ressurs<PaginableResponse<TaskDto>> {
        logger.info("$saksbehandlerId henter ${type?.plus("-") ?: ""}tasker med status $statuses")
        return hentTasksGittSpørring(page) { pageRequest: PageRequest ->
            taskService.finnTasksMedStatus(
                statuses,
                type,
                pageRequest,
            )
        }
            .fold(
                onSuccess = { Ressurs.success(data = it) },
                onFailure = { e ->
                    logger.error("Henting av tasker feilet", e)
                    Ressurs.failure(errorMessage = "Henting av tasker med status '$statuses', feilet.", error = e)
                },
            )
    }

    fun hentTasksGittSpørring(
        page: Int,
        spørring: (PageRequest) -> List<Task>,
    ): Result<PaginableResponse<TaskDto>> = Result.runCatching {
        val pageRequest = PageRequest.of(page, TASK_LIMIT, Sort.Direction.DESC, "opprettetTid")
        val tasks = spørring.invoke(pageRequest)
        val taskLoggMetadata = taskService.finnTaskLoggMetadata(tasks.map { it.id })
        PaginableResponse(
            tasks.map {
                val taskLogg = taskLoggMetadata[it.id]
                TaskDto(
                    id = it.id,
                    status = it.status,
                    avvikstype = it.avvikstype,
                    opprettetTidspunkt = it.opprettetTid,
                    triggerTid = it.triggerTid,
                    taskStepType = it.type,
                    metadata = it.metadata,
                    payload = it.payload,
                    antallLogger = taskLogg?.antallLogger ?: 0,
                    sistKjørt = taskLogg?.sistOpprettetTid,
                    kommentar = taskLogg?.sisteKommentar,
                    callId = it.callId,
                )
            },
        )
    }

    fun hentTaskLogg(id: Long, saksbehandlerId: String): Ressurs<List<TaskloggDto>> {
        logger.info("$saksbehandlerId henter tasklogg til task=$id")

        return Result.runCatching {
            val taskLogg = taskService.findTaskLoggByTaskId(id)
            taskLogg.sortedByDescending { it.opprettetTid }
                .map { TaskloggDto(it.id, it.endretAv, it.type, it.node, it.melding, it.opprettetTid) }
        }
            .fold(
                onSuccess = { Ressurs.success(data = it) },
                onFailure = { e ->
                    logger.error("Henting av tasker feilet", e)
                    Ressurs.failure(errorMessage = "Henting av tasklogg feilet.", error = e)
                },
            )
    }

    @Transactional
    fun rekjørTask(taskId: Long, saksbehandlerId: String): Ressurs<String> {
        val task: Task = taskService.findById(taskId)

        taskService.klarTilPlukk(task.medTriggerTid(LocalDateTime.now()), saksbehandlerId)
        logger.info("$saksbehandlerId rekjører task $taskId")

        return Ressurs.success(data = "")
    }

    @Transactional
    fun rekjørTasks(status: Status, saksbehandlerId: String): Ressurs<String> {
        logger.info("$saksbehandlerId rekjører alle tasks med status $status")

        return Result.runCatching {
            taskService.finnTasksMedStatus(listOf(status))
                .map { taskService.klarTilPlukk(it.medTriggerTid(LocalDateTime.now()), saksbehandlerId) }
        }
            .fold(
                onSuccess = { Ressurs.success(data = "") },
                onFailure = { e ->
                    logger.error("Rekjøring av tasker med status '$status' feilet", e)
                    Ressurs.failure(errorMessage = "Rekjøring av tasker med status '$status' feilet", error = e)
                },
            )
    }

    @Transactional
    fun avvikshåndterTask(
        taskId: Long,
        avvikstype: Avvikstype,
        årsak: String,
        saksbehandlerId: String,
    ): Ressurs<String> {
        val task: Task = taskService.findById(taskId)

        logger.info("$saksbehandlerId setter task $taskId til avvikshåndtert", taskId)

        return Result.runCatching {
            taskService.avvikshåndter(
                task = task,
                avvikstype = avvikstype,
                årsak = årsak,
                endretAv = saksbehandlerId,
            )
        }
            .fold(
                onSuccess = {
                    Ressurs.success(data = "")
                },
                onFailure = { e ->
                    logger.error("Avvikshåndtering av $taskId feilet", e)
                    Ressurs.failure(errorMessage = "Avvikshåndtering av $taskId feilet", error = e)
                },
            )
    }

    @Transactional
    fun kommenterTask(taskId: Long, kommentarDTO: KommentarDTO, saksbehandlerId: String): Ressurs<String> {
        val task: Task = taskService.findById(taskId)

        logger.info("$saksbehandlerId legger inn kommentar på task $taskId", taskId)

        return Result.runCatching {
            taskService.kommenter(
                task = task,
                kommentar = kommentarDTO.kommentar,
                endretAv = saksbehandlerId,
                settTilManuellOppfølgning = kommentarDTO.settTilManuellOppfølging,
            )
        }
            .fold(
                onSuccess = {
                    Ressurs.success(data = "")
                },
                onFailure = { e ->
                    logger.error("Kommentering av $taskId feilet", e)
                    Ressurs.failure(errorMessage = "Kommentering av $taskId feilet", error = e)
                },
            )
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(RestTaskService::class.java)
        const val TASK_LIMIT: Int = 100
    }
}
