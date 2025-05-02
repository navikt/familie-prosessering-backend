# Familie-prosessering

Prosesseringsmotor for familieområdet.

## Utvikling
Formatter kode med `mvn antrun:run@ktlint-format`

## Generellt
* Oppdater status i task-tabellen til lengde 20: `ALTER TABLE task ALTER COLUMN status VARCHAR(20)  DEFAULT 'UBEHANDLET'::CHARACTER VARYING NOT NULL;`

I prosessering oppretter man en task (en jobb).
Hver task har en type, som gjør at prosessering kan bruke riktig `AsyncTaskStep` for å håndeter akkurat den tasken.

En task lagres i databasen og blir plukket opp av biblioteket, og kaller riktig `AsyncTaskStep` for den typen.

### Eksempel

```kotlin
@Service
class Foo(
    val taskService: TaskService
) {
    fun opprettOppgave(data: Data) {
        taskService.save(FooTask.opprettTask(data))
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = FooTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Oppretter oppgave for julenissen",
)
class FooTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        oppgaveService.opprettOppgave(...)
        //...
    }

    companion object {
        const val TYPE = "Foo"
        fun opprettTask(data: Data): Task {
            // payload er en streng, men kan også være en serialisert json, 
            // som då kan plukkes opp i doTask
            return Task(type = TYPE, payload = "", properties = Properties())
                .medTriggerTid(LocalDateTime.now())
        }
    }
}
```

## [TaskStepBeskrivelse](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/TaskStepBeskrivelse.kt)
#### `maxAntallFeil`
* Tasken rekjøres `maxAntallFeil` ganger før den settes til `FEILET` eller `MANUELL_OPPFØLGING`

#### `taskStepType`
* Unikt tasknavn som lagres ned i databasen (`Task.type`) og brukes for å finne riktig klasse som skal utføre tasken. 

#### `beskrivelse`
* Enkel beskrivelse av hva tasken er ment å utføre. 

#### `triggerTidVedFeilISekunder`
* Dersom tasken feiler vil den vente i `triggerTidVedFeilISekunder` sekunder før den (tidligst) prøver å rekjøre neste gang

#### `settTilManuellOppfølgning`
* Dersom tasken feiler og ikke lenger skal rekjøres får den status `FEILET` som utgangspunkt. Da vil den bli forsøkt rekjørt neste dag, men for noen tasker må det en manuell prosess til før noe går igjennom. Da kan man sette `settTilManuellOppfølgning` så vil den gå direkte til `MANUELL_OPPFØLGING` istedenfor `FEILET`

## Prosessering av tasks
* [TaskStepExecutorService](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/internal/TaskStepExecutorService.kt)
plukker opp tasks som skal prosesseres, basert på status og trigger-tid, og køer opp de . 

Håndtering av tasks fra køen
* `ThreadPoolTaskExecutor` tar en task fra køen
* Setter LogContext på tråden, setter `callId` i `MDC` sånn at man får sporet all logg for den tasken
* Markerer den som plukket sånn at ikke andre tråder eller podder plukker samme task
* Sender tasken til `TaskWorker` for prosessering av tasken
  * Ved en ev. feil kalles `doFeilhåndtering`

### [TaskWorker](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/internal/TaskWorker.kt)
TaskWorker har metoder for prosessering av tasken

#### doActualWork
* Setter status `BEHANDLER` på tasken
* Finner riktig `AsyncTaskStep` for gitt tasktype og kaller på de ulike metodene for å behandle tasken
* Setter status `FERDIG` på tasken
* PS: Siden `transactional` benyttes, så vil aldri tasken ha status `BEHANDLER` i databasen. Den går rett fra `PLUKKET` til `FERDIG` 

### doFeilhåndtering
* Lagrer en TaskLogg om at tasken har feilet

Hvis en task ikke har feilet flere ganger enn hva som er definiert som maks antall feil i `TaskStepBeskrivelse`, 
så vil tasken få ny status `KLAR_TIL_PLUKK` og bli rekjørt senere.

## [Task](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/domene/Task.kt)

Hver task får en callId som settes settes i `MDC` for å kunne spore logger og kall for den tasken.

### CallId

Dersom man har en task som skal opprette flere nye tasks, så kan det ofte være hensiktsmessig å gi hver ny task en ny
callId, for å enklere kunne spore hver task.
For å gjøre dette må man erstatte properties etter at man opprettet tasken fordi konstruktorn alltid overskriver callId.

```kotlin
val properties = Properties().apply {
    setProperty("behandlingId", behandlingId.toString())
    setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
}
return Task(type = TYPE, payload = "", properties = Properties())
    .copy(metadataWrapper = PropertiesWrapper(properties))
```

### Exceptions

#### [TaskExceptionUtenStackTrace](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/error/TaskExceptionUtenStackTrace.kt)

Hvis man ønsker å kaste en exception uten noen stacktrace fra sin `AsyncTaskStep` så kan man kaste en
`TaskExceptionUtenStackTrace` som ikke gir en lang stack trace i prosessering-GUI'et når man ser på en task.

#### [RekjørSenereException](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/error/RekjørSenereException.kt)
I noen tilfeller ønsker man å rekjøre tasken senere og ikke med en gang. Da kan tasken kaste denne feilen og spesifisere triggertid for når den skal kjøres neste gang.

#### [MaxAntallRekjøringerException](prosessering-core/src/main/kotlin/no/nav/familie/prosessering/error/RekjørSenereException.kt)
Denne skal ikke kastes, men er egentlig en dto for å serialisere og dokumentere i TaskLogg at en task har feilet og nådd maks antall rekjøringer.  

## Oppsett

### Properties
Alle properties har defaultverdier.
```properties
prosessering:
    # mulighet får å skru av prosessering
    enabled: true
    # antall tasks som prosesseres samtidig
    maxAntall: 10
    # Antall ms mellom hver polling av tasks fra databasen
    fixedDelayString.in.milliseconds: 30000
    
    # antall tasks som plukkes opp fra databasen
    queue.capacity: 20
    # antall parallelle tasks som kjøres 
    pool.size: 4

    # Se egen dokumentasjon om continuousRunning
    continuousRunning.enabled: false

    # sletter historiske tasks - med status ferdig
    delete
        # antall uker
        after.weeks: 2
        # antall tasks som plukkes opp for sletting 
        pagesize: 10000
    
    # tidspunkt for når jobb som rekjører feilede tasks skal kjøre
    cronRetryTasks: 0 0 7 1/1 * ?
```

#### Property `continuousRunning.enabled`
Tasks blir plukket opp hver `fixedDelayString.in.milliseconds` ms. 
Når tasks er plukket og kjørt ferdig, så venter systemet `fixedDelayString.in.milliseconds` til neste plukk av tasker.

Hvis `continuousRunning.enabled` settes til true, så poller systemet umiddelbart etter kjøring av de forrige taskene.

### SQL
[Eksempel på oppsett av skjema](prosessering-core/src/test/resources/db/migration/V1__schema.sql)

### JDBC
* Må sette opp converters, eks:
```kotlin
@Configuration
class DatabaseConfiguration : AbstractJdbcConfiguration() {

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(listOf(StringTilPropertiesWrapperConverter(),
                                            PropertiesWrapperTilStringConverter()))
    }
}
```

### ProsesseringInfoProvider
For å kunne fjerne avhengigheter til felles familie-repoer må man selv konfigurere en bean for `ProsesseringInfoProvider`
Eksempel på implementasjon, husk at `SikkerhetsContext.hentSaksbehandlerNavn()` ofte henter `name` fra `claims` og ikke `preferred_username`
```kotlin
@Bean
fun prosesseringInfoProvider() = object : ProsesseringInfoProvider {
    override fun hentBrukernavn(): String = try {
        SpringTokenValidationContextHolder().tokenValidationContext.getClaims("azuread").getStringClaim("preferred_username")
    } catch (e: Exception) {
        SikkerhetContext.SYSTEM_FORKORTELSE
    }

    override fun harTilgang(): Boolean = grupper().contains("<id for AdGruppe som skal ha tilgang>")

    private fun grupper(): List<String> {
        return try {
            SpringTokenValidationContextHolder().tokenValidationContext.getClaims("azuread")
                ?.get("groups") as List<String>? ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun isLeaader(): Boolean = LeaderClient.isLeader() ?: true
}
```

![](https://github.com/navikt/familie-prosessering-backend/workflows/Build-Deploy/badge.svg)
