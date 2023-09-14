# Familie-prosessering

Prosesseringsmotor for familieområdet.

## Generellt
* Oppdater status i task-tabellen til lengde 20: `ALTER TABLE task ALTER COLUMN status VARCHAR(20)  DEFAULT 'UBEHANDLET'::CHARACTER VARYING NOT NULL;`


## JDBC
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

## ProsesseringInfoProvider
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
