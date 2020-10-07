package no.nav.familie.prosessering.rest

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Avvikstype
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RestTaskService(private val taskRepository: TaskService) {

    fun hentTasks(statuses: List<Status>, saksbehandlerId: String, page: Int): Ressurs<List<ITask>> {
        logger.info("$saksbehandlerId henter tasker med status $statuses")

        return Result.runCatching {
            taskRepository.finnTasksMedStatus(statuses, PageRequest.of(page, TASK_LIMIT))
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure(errorMessage = "Henting av tasker med status '$statuses', feilet.", error = e)
                        }
                )
    }

    fun hentTasks2(statuses: List<Status>, saksbehandlerId: String, page: Int): Ressurs<PaginableResponse<TaskDto>> {
        logger.info("$saksbehandlerId henter tasker med status $statuses")

        return Result.runCatching {
            PaginableResponse(taskRepository.finnTasksTilFrontend(statuses, PageRequest.of(page, TASK_LIMIT)).map {
                TaskDto(it.id,
                        it.status,
                        it.avvikstype,
                        it.opprettetTid,
                        it.triggerTid,
                        it.type,
                        it.metadataProperties(),
                        it.payload,
                        it.logg.size,
                        it.logg.maxByOrNull { logg -> logg.opprettetTid }?.opprettetTid,
                        it.callId)
            })
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure(errorMessage = "Henting av tasker med status '$statuses', feilet.", error = e)
                        }
                )
    }

    fun hentTaskLogg(id: Long, saksbehandlerId: String): Ressurs<List<TaskloggDto>> {
        logger.info("$saksbehandlerId henter tasklogg til task=$id")

        return Result.runCatching {
            val task = taskRepository.findById(id)
            task.logg.map { TaskloggDto(it.id, it.endretAv, it.type, it.node, it.melding, it.opprettetTid) }
        }
                .fold(
                        onSuccess = { Ressurs.success(data = it) },
                        onFailure = { e ->
                            logger.error("Henting av tasker feilet", e)
                            Ressurs.failure(errorMessage = "Henting av tasklogg feilet.", error = e)
                        }
                )
    }

    @Transactional
    fun rekjørTask(taskId: Long, saksbehandlerId: String): Ressurs<String> {
        val task: ITask = taskRepository.findById(taskId)

        taskRepository.save(task.klarTilPlukk(saksbehandlerId))
        logger.info("$saksbehandlerId rekjører task $taskId")

        return Ressurs.success(data = "")

    }

    @Transactional
    fun rekjørTasks(status: Status, saksbehandlerId: String): Ressurs<String> {
        logger.info("$saksbehandlerId rekjører alle tasks med status $status")

        return Result.runCatching {
            taskRepository.finnTasksMedStatus(listOf(status), Pageable.unpaged())
                    .map { taskRepository.save(it.klarTilPlukk(saksbehandlerId)) }
        }
                .fold(
                        onSuccess = { Ressurs.success(data = "") },
                        onFailure = { e ->
                            logger.error("Rekjøring av tasker med status '$status' feilet", e)
                            Ressurs.failure(errorMessage = "Rekjøring av tasker med status '$status' feilet", error = e)
                        }
                )
    }

    @Transactional
    fun avvikshåndterTask(taskId: Long, avvikstype: Avvikstype, årsak: String, saksbehandlerId: String): Ressurs<String> {
        val task: ITask = taskRepository.findById(taskId)

        logger.info("$saksbehandlerId setter task $taskId til avvikshåndtert", taskId)

        return Result.runCatching { taskRepository.save(task.avvikshåndter(avvikstype, årsak, saksbehandlerId)) }
                .fold(
                        onSuccess = {
                            Ressurs.success(data = "")
                        },
                        onFailure = { e ->
                            logger.error("Avvikshåndtering av $taskId feilet", e)
                            Ressurs.failure(errorMessage = "Avvikshåndtering av $taskId feilet", error = e)
                        }
                )
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(RestTaskService::class.java)
        const val TASK_LIMIT: Int = 100
    }
}
