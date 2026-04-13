package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.api.TaskApiFasade
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.domene.Status
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
class TaskControllerNavTokenSupport(
    restTaskService: RestTaskService,
    prosesseringInfoProvider: ProsesseringInfoProvider,
) {
    private val api = TaskApiFasade(restTaskService, prosesseringInfoProvider)

    @GetMapping(path = ["/task/{id}"])
    fun taskMedId(
        @PathVariable id: Long,
    ): ResponseEntity<Ressurs<TaskDto>> = ResponseEntity.ok(api.hentTaskMedId(id))

    @GetMapping(path = ["task/v2"])
    fun task2(
        @RequestParam status: Status?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) type: String? = null,
    ): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> = ResponseEntity.ok(api.hentTasks(status, page, type))

    @GetMapping(path = ["task/callId/{callId}"])
    fun tasksForCallId(
        @PathVariable callId: String,
    ): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> = ResponseEntity.ok(api.hentTasksForCallId(callId))

    @GetMapping(path = ["/task/ferdigNaaFeiletFoer"])
    fun tasksSomErFerdigNåMenFeiletFør(): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> =
        ResponseEntity.ok(api.hentTasksSomErFerdigNåMenFeiletFør())

    @GetMapping(path = ["/task/antall-til-oppfolging"])
    fun antallTilOppfølging(): ResponseEntity<Ressurs<Long>> = ResponseEntity.ok(api.finnAntallTaskerSomKreverOppfølging())

    @GetMapping(path = ["/task/antall-feilet-og-manuell-oppfolging"])
    fun antallFeiletOgManuellOppfølging(): ResponseEntity<
        Ressurs<no.nav.familie.prosessering.internal.TaskerMedStatusFeiletOgManuellOppfølging>,
    > =
        ResponseEntity.ok(
            api.finnAntallTaskerMedStatusFeiletOgManuellOppfølging(),
        )

    @GetMapping(path = ["/task/logg/{id}"])
    fun tasklogg(
        @PathVariable id: Long,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<Ressurs<List<TaskloggDto>>> = ResponseEntity.ok(api.hentTaskLogg(id, page))

    @PutMapping(path = ["/task/rekjor"])
    fun rekjørTask(
        @RequestParam taskId: Long,
    ): ResponseEntity<Ressurs<String>> = ResponseEntity.ok(api.rekjørTask(taskId))

    @PutMapping(path = ["task/rekjorAlle"])
    fun rekjørTasks(
        @RequestHeader status: Status,
        @RequestHeader(required = false) type: String? = null,
    ): ResponseEntity<Ressurs<String>> = ResponseEntity.ok(api.rekjørTasks(status, type))

    @PutMapping(path = ["/task/avvikshaandter"])
    fun avvikshåndterTask(
        @RequestParam taskId: Long,
        @RequestBody avvikshåndterDTO: AvvikshåndterDTO,
    ): ResponseEntity<Ressurs<String>> = ResponseEntity.ok(api.avvikshåndterTask(taskId, avvikshåndterDTO))

    @GetMapping(path = ["/task/type/alle"])
    fun hentAlleTasktyper(): ResponseEntity<Ressurs<List<String>>> = ResponseEntity.ok(api.hentAlleTasktyper())

    @PutMapping(path = ["/task/kommenter"])
    fun kommenterTask(
        @RequestParam taskId: Long,
        @RequestBody kommentarDTO: KommentarDTO,
    ): ResponseEntity<Ressurs<String>> = ResponseEntity.ok(api.kommenterTask(taskId, kommentarDTO))
}
