# prosessering-web-nav-token-support

Denne modulen eksponerer task-API-et i prosessering over HTTP og beskytter endepunktene med NAV sitt `token-support`-bibliotek.

Modulen passer for applikasjoner som allerede bruker `no.nav.security.token-support`, og som ønsker å validere token med `@ProtectedWithClaims` i stedet for å sette opp en egen Spring Security resource server-kjede for task-API-et.

## Hva modulen dekker

Modulen leverer:

* en ferdig `TaskControllerNavTokenSupport` med endepunkter under `/api/task/**`
* tokenvalidering via `@ProtectedWithClaims(issuer = "azuread")`
* et ekstra autorisasjonsfilter som sjekker `ProsesseringInfoProvider.harTilgang()`

Det betyr at modulen både beskytter task-API-et med tokenvalidering og legger på en eksplisitt tilgangssjekk for brukeren som kaller API-et.

## Hvordan sikkerhet og tokenvalidering fungerer

Controlleren er annotert med:

```kotlin
@ProtectedWithClaims(issuer = "azuread")
```

Det innebærer at `token-support` validerer innkommende token før requesten får treffe controlleren.

I tillegg har modulen et `OncePerRequestFilter` (`ProsesseringUserAuthorizationFilter`) som kjører for kall til `/api/task/**` og gjør følgende:

1. Kaller `ProsesseringInfoProvider.harTilgang()`.
2. Hvis svaret er `true`, får requesten gå videre.
3. Hvis svaret er `false`, returneres `401 Unauthorized`.

Kort sagt:

* `token-support` bekrefter at tokenet er gyldig
* `ProsesseringInfoProvider` avgjør om brukeren faktisk skal ha tilgang

## Hva konsumenten må gjøre

### 1. Legg til modulen som dependency

```xml
<dependency>
    <groupId>no.nav.familie.prosessering</groupId>
    <artifactId>prosessering-web-nav-token-support</artifactId>
</dependency>
```

### 2. Sett opp `token-support` i applikasjonen

Modulen forutsetter at konsumenten allerede har konfigurert NAV sitt `token-support`-oppsett for issuer `azuread`.

Selve detaljene i denne konfigurasjonen ligger hos konsumenten. Modulen definerer ikke issuer-oppsettet selv; den forventer bare at det finnes og at aliaset heter `azuread`.

### 3. Registrer en bean for `ProsesseringInfoProvider`

Konsumenten må selv implementere `ProsesseringInfoProvider` fra `prosessering-core`.

Denne brukes til:

* å hente brukernavn til logging
* å avgjøre om brukeren har tilgang til task-API-et

Et minimumsoppsett kan se slik ut:

```kotlin
@Bean
fun prosesseringInfoProvider() = object : ProsesseringInfoProvider {
    override fun hentBrukernavn(): String = "ukjent"

    override fun harTilgang(): Boolean = TODO("Sjekk claims, roller eller grupper")
}
```

Se README-en i rotmappen for et mer konkret eksempel på claims-oppslag.

## Når bør du velge denne modulen?

Velg `prosessering-web-nav-token-support` når applikasjonen deres allerede bruker NAV sitt `token-support`-bibliotek og ønsker å holde seg til samme sikkerhetsmodell.

Hvis dere heller vil bruke Spring Security sin resource server direkte med issuer/audience-konfigurasjon, er `prosessering-web-spring-security` et bedre valg.
