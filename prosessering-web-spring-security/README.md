# prosessering-web-spring-security

Denne modulen eksponerer task-API-et i prosessering over HTTP og beskytter endepunktene med Spring Security sin resource server-støtte for JWT.

Modulen passer for applikasjoner som allerede bruker Spring Security, eller som ønsker å validere Azure AD-token direkte via issuer/JWKS uten `token-support`-annotasjonene fra NAV-bibliotekene.

## Hva modulen dekker

Modulen leverer:

* en ferdig `TaskControllerSpringSecurity` med endepunkter under `/api/task/**`
* en `SecurityFilterChain` som kun gjelder for `/api/task/**`
* JWT-validering av innkommende bearer-token
* validering av både `issuer` og `audience`
* en ekstra tilgangssjekk mot `ProsesseringInfoProvider.harTilgang()`

Det betyr at konsumenten slipper å lage egen controller eller egen sikkerhetskonfigurasjon for task-API-et, så lenge standardoppsettet i denne modulen er tilstrekkelig.

## Hvordan sikkerheten fungerer

Sikkerheten settes opp i `ProsesseringTaskApiSecurityConfig`.

For kall til `/api/task/**` skjer dette:

1. Spring Security leser bearer-token fra requesten.
2. `ProsesseringJwtDecoder` validerer tokenet mot `AZURE_OPENID_CONFIG_ISSUER`.
3. Samme decoder krever at `aud`-claim inneholder `AZURE_APP_CLIENT_ID`.
4. Requesten må være autentisert.
5. `ProsesseringInfoProvider.harTilgang()` må returnere `true`.

Hvis ett av disse stegene feiler, blir requesten avvist før controlleren kjøres.

## Hva konsumenten må gjøre

For å bruke modulen må konsumenten fortsatt gjøre tre ting.

### 1. Legg til modulen som dependency

```xml
<dependency>
    <groupId>no.nav.familie</groupId>
    <artifactId>prosessering-web-spring-security</artifactId>
</dependency>
```

### 2. Sett opp properties for tokenvalidering

Modulen forventer følgende properties:

```properties
AZURE_OPENID_CONFIG_ISSUER=<issuer-url>
AZURE_APP_CLIENT_ID=<client-id-som-skal-finnes-i-aud>
```

`AZURE_OPENID_CONFIG_ISSUER` brukes for å hente OpenID-konfigurasjon/JWKS.

`AZURE_APP_CLIENT_ID` brukes som forventet audience. Tokenet må altså ha denne verdien i `aud`.

### 3. Registrer en bean for `ProsesseringInfoProvider`

Konsumenten må selv implementere `ProsesseringInfoProvider` fra `prosessering-core`.

Denne brukes av modulen til:

* å hente brukernavn til logging
* å avgjøre om innlogget bruker faktisk har tilgang til task-API-et

Et minimumsoppsett ser slik ut:

```kotlin
@Bean
fun prosesseringInfoProvider() = object : ProsesseringInfoProvider {
    override fun hentBrukernavn(): String = "ukjent"

    override fun harTilgang(): Boolean = TODO("Sjekk claims, roller eller grupper")
}
```

Se README-en i rotmappen for et mer konkret eksempel på hvordan claims kan leses ut.

## Når bør du velge denne modulen?

Velg `prosessering-web-spring-security` når applikasjonen deres ønsker å bruke vanlig Spring Security resource server for tokenvalidering.

Hvis applikasjonen i stedet allerede bruker NAV sitt `token-support`-oppsett og `@ProtectedWithClaims`, er `prosessering-web-nav-token-support` ofte et mer naturlig valg.
