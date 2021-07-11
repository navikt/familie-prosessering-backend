package no.nav.familie.prosessering.rest

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
              @RequestParam(required = false) page: Int?): Ressurs<PaginableResponse<TaskDto>> {
        val statuser: List<Status> = status?.let { listOf(it) } ?: Status.values().toList()
        return restTaskService.hentTasks(statuser, hentBrukernavn(), page ?: 0)
    }

    @GetMapping(path = ["/task/logg/{id}"])
    fun tasklogg(@PathVariable id: Long,
                 @RequestParam(required = false) page: Int?): Ressurs<List<TaskloggDto>> {
        return restTaskService.hentTaskLogg(id, hentBrukernavn())
    }

    @PutMapping(path = ["/task/rekjor"])
    fun rekjørTask(@RequestParam taskId: Long): Ressurs<String> {
        return restTaskService.rekjørTask(taskId, hentBrukernavn())
    }

    @PutMapping(path = ["task/rekjorAlle"])
    fun rekjørTasks(@RequestHeader status: Status): Ressurs<String> {
        return restTaskService.rekjørTasks(status, hentBrukernavn())
    }

    @PutMapping(path = ["/task/avvikshaandter"])
    fun avvikshåndterTask(@RequestParam taskId: Long,
                          @RequestBody avvikshåndterDTO: AvvikshåndterDTO): Ressurs<String> {
        return restTaskService.avvikshåndterTask(taskId,
                                                 avvikshåndterDTO.avvikstype,
                                                 avvikshåndterDTO.årsak,
                                                 hentBrukernavn())
    }

    @GetMapping("task/statistikk/status")
    fun statusHistorikk(): Map<Status, Int> {
        return restTaskService.hentStatusStatistikk()
    }
}
