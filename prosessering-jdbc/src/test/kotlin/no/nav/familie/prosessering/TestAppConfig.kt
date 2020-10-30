package no.nav.familie.prosessering

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.h2.jdbcx.JdbcDataSource
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension
import javax.sql.DataSource

@SpringBootConfiguration
@EnableJdbcRepositories("no.nav.familie")
@ComponentScan("no.nav.familie")
class TestAppConfig : JdbcRepositoryConfigExtension() {

//    @Bean
//    fun dataSource(): DataSource {
//
//        return JdbcDataSource().apply {
//            this.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=POSTGRESQL;DB_CLOSE_ON_EXIT=FALSE")
//        }
//    }

    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder {
        return object : TokenValidationContextHolder {
            override fun getTokenValidationContext(): TokenValidationContext {
                return TokenValidationContext(emptyMap())
            }

            override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext) {}
        }
    }

}
