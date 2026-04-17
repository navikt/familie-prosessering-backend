package no.nav.familie.prosessering.api

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskerMedStatusFeiletOgManuellOppfølging
import no.nav.familie.prosessering.rest.AvvikshåndterDTO
import no.nav.familie.prosessering.rest.KommentarDTO
import no.nav.familie.prosessering.rest.PaginableResponse
import no.nav.familie.prosessering.rest.Ressurs
import no.nav.familie.prosessering.rest.RestTaskService
import no.nav.familie.prosessering.rest.TaskDto
import no.nav.familie.prosessering.rest.TaskloggDto
import org.springframework.stereotype.Component

/**
 * Web-free fasade for Task API-et.
 *
 * Konsumenter kan selv velge hvilken web-security stack de vil bruke, og kalle dette fra controllers.
 */
@Component
class TaskApiFasade(
    private val restTaskService: RestTaskService,
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) {
    private fun brukernavn(): String = prosesseringInfoProvider.hentBrukernavn()

    fun hentTaskMedId(id: Long): Ressurs<TaskDto>? = restTaskService.hentTaskMedId(id, brukernavn())

    fun hentTasks(
        status: Status?,
        page: Int?,
        type: String?,
    ): Ressurs<PaginableResponse<TaskDto>> {
        val statuser: List<Status> = status?.let { listOf(it) } ?: Status.values().toList()
        return restTaskService.hentTasks(statuser, brukernavn(), page ?: 0, type)
    }

    fun hentTasksForCallId(callId: String): Ressurs<PaginableResponse<TaskDto>>? = restTaskService.hentTasksForCallId(callId, brukernavn())

    fun hentTasksSomErFerdigNåMenFeiletFør(): Ressurs<PaginableResponse<TaskDto>>? =
        restTaskService.hentTasksSomErFerdigNåMenFeiletFør(brukernavn())

    fun finnAntallTaskerSomKreverOppfølging(): Ressurs<Long> = restTaskService.finnAntallTaskerSomKreverOppfølging()

    fun finnAntallTaskerMedStatusFeiletOgManuellOppfølging(): Ressurs<TaskerMedStatusFeiletOgManuellOppfølging> =
        restTaskService.finnAntallTaskerMedStatusFeiletOgManuellOppfølging()

    fun hentTaskLogg(id: Long): Ressurs<List<TaskloggDto>> = restTaskService.hentTaskLogg(id, brukernavn())

    fun rekjørTask(taskId: Long): Ressurs<String> = restTaskService.rekjørTask(taskId, brukernavn())

    fun rekjørTasks(
        status: Status,
        type: String?,
    ): Ressurs<String> = restTaskService.rekjørTasks(status, type, brukernavn())

    fun avvikshåndterTask(
        taskId: Long,
        dto: AvvikshåndterDTO,
    ): Ressurs<String> = restTaskService.avvikshåndterTask(taskId, dto.avvikstype, dto.årsak, brukernavn())

    fun hentAlleTasktyper(): Ressurs<List<String>> = restTaskService.hentAlleTasktyper()

    fun kommenterTask(
        taskId: Long,
        dto: KommentarDTO,
    ): Ressurs<String> = restTaskService.kommenterTask(taskId, dto, brukernavn())
}
