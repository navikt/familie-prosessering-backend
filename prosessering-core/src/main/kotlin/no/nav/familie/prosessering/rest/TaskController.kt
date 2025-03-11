package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskerMedStatusFeiletOgManuellOppfølging
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TaskController(
    private val restTaskService: RestTaskService,
    private val prosesseringInfoProvider: ProsesseringInfoProvider,
) {
    fun hentBrukernavn(): String = prosesseringInfoProvider.hentBrukernavn()

    @GetMapping(path = ["/task/{id}"])
    fun taskMedId(
        @PathVariable id: Long,
    ): ResponseEntity<Ressurs<TaskDto>> = ResponseEntity.ok(restTaskService.hentTaskMedId(id, hentBrukernavn()))

    @GetMapping(path = ["task/v2"])
    fun task2(
        @RequestParam status: Status?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) type: String? = null,
    ): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> {
        val statuser: List<Status> = status?.let { listOf(it) } ?: Status.values().toList()
        return ResponseEntity.ok(restTaskService.hentTasks(statuser, hentBrukernavn(), page ?: 0, type))
    }

    @GetMapping(path = ["task/callId/{callId}"])
    fun tasksForCallId(
        @PathVariable callId: String,
    ): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> = ResponseEntity.ok(restTaskService.hentTasksForCallId(callId, hentBrukernavn()))

    @GetMapping(path = ["/task/ferdigNaaFeiletFoer"])
    fun tasksSomErFerdigNåMenFeiletFør(): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> =
        ResponseEntity.ok(restTaskService.hentTasksSomErFerdigNåMenFeiletFør(hentBrukernavn()))

    @GetMapping(path = ["/task/antall-til-oppfolging"])
    fun antallTilOppfølging(): ResponseEntity<Ressurs<Long>> = ResponseEntity.ok(restTaskService.finnAntallTaskerSomKreverOppfølging())

    @GetMapping(path = ["/task/antall-feilet-og-manuell-oppfolging"])
    fun antallFeiletOgManuellOppfølging(): ResponseEntity<Ressurs<TaskerMedStatusFeiletOgManuellOppfølging>> =
        ResponseEntity.ok(restTaskService.finnAntallTaskerMedStatusFeiletOgManuellOppfølging())

    @GetMapping(path = ["/task/logg/{id}"])
    fun tasklogg(
        @PathVariable id: Long,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<Ressurs<List<TaskloggDto>>> = ResponseEntity.ok(restTaskService.hentTaskLogg(id, hentBrukernavn()))

    @PutMapping(path = ["/task/rekjor"])
    fun rekjørTask(
        @RequestParam taskId: Long,
    ): ResponseEntity<Ressurs<String>> = ResponseEntity.ok(restTaskService.rekjørTask(taskId, hentBrukernavn()))

    @PutMapping(path = ["task/rekjorAlle"])
    fun rekjørTasks(
        @RequestHeader status: Status,
    ): ResponseEntity<Ressurs<String>> = ResponseEntity.ok(restTaskService.rekjørTasks(status, hentBrukernavn()))

    @PutMapping(path = ["/task/avvikshaandter"])
    fun avvikshåndterTask(
        @RequestParam taskId: Long,
        @RequestBody avvikshåndterDTO: AvvikshåndterDTO,
    ): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(
            restTaskService.avvikshåndterTask(
                taskId,
                avvikshåndterDTO.avvikstype,
                avvikshåndterDTO.årsak,
                hentBrukernavn(),
            ),
        )

    @GetMapping(path = ["/task/type/alle"])
    fun hentAlleTasktyper(): ResponseEntity<Ressurs<List<String>>> = ResponseEntity.ok(restTaskService.hentAlleTasktyper())

    @PutMapping(path = ["/task/kommenter"])
    fun kommenterTask(
        @RequestParam taskId: Long,
        @RequestBody kommentarDTO: KommentarDTO,
    ): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(
            restTaskService.kommenterTask(
                taskId,
                kommentarDTO,
                hentBrukernavn(),
            ),
        )
}
