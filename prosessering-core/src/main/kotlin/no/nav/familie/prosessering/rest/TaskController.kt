package no.nav.familie.prosessering.rest

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TaskController(private val restTaskService: RestTaskService, private val oidcUtil: OIDCUtil) {

    fun hentBrukernavn(): String {
        return oidcUtil.getClaim("preferred_username")
    }

    @GetMapping(path = ["/v2/task", "task/v2"])
    fun task2(@RequestParam status: Status?,
              @RequestParam(required = false) page: Int?,
              @RequestParam(required = false) type: String? = null): ResponseEntity<Ressurs<PaginableResponse<TaskDto>>> {
        val statuser: List<Status> = status?.let { listOf(it) } ?: Status.values().toList()
        return ResponseEntity.ok(restTaskService.hentTasks(statuser, hentBrukernavn(), page ?: 0, type))
    }

    @GetMapping(path = ["/task/antall-til-oppfolging"])
    fun antallTilOppfølging(): ResponseEntity<Ressurs<Long>> {
        return ResponseEntity.ok(restTaskService.finnAntallTaskerSomKreverOppfølging())
    }

    @GetMapping(path = ["/task/logg/{id}"])
    fun tasklogg(@PathVariable id: Long,
                 @RequestParam(required = false) page: Int?): ResponseEntity<Ressurs<List<TaskloggDto>>> {
        return ResponseEntity.ok(restTaskService.hentTaskLogg(id, hentBrukernavn()))
    }

    @PutMapping(path = ["/task/rekjor"])
    fun rekjørTask(@RequestParam taskId: Long): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.rekjørTask(taskId, hentBrukernavn()))
    }

    @PutMapping(path = ["task/rekjorAlle"])
    fun rekjørTasks(@RequestHeader status: Status): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.rekjørTasks(status, hentBrukernavn()))
    }

    @PutMapping(path = ["/task/avvikshaandter"])
    fun avvikshåndterTask(@RequestParam taskId: Long,
                          @RequestBody avvikshåndterDTO: AvvikshåndterDTO): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.avvikshåndterTask(taskId,
                                                                   avvikshåndterDTO.avvikstype,
                                                                   avvikshåndterDTO.årsak,
                                                                   hentBrukernavn()))
    }

    @PutMapping(path = ["/task/kommenter"])
    fun kommenterTask(@RequestParam taskId: Long,
                          @RequestBody kommentar: String): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.ok(restTaskService.kommenterTask(taskId,
                                                                   kommentar,
                                                                   hentBrukernavn()))
    }
}
