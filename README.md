# Familie-prosessering

Prosesseringsmotor for familieområdet.

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

## JPA

![](https://github.com/navikt/familie-prosessering-backend/workflows/Build-Deploy/badge.svg)
