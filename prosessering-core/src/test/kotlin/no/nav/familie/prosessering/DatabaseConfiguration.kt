package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.Prioritet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.data.convert.CustomConversions
import org.springframework.data.jdbc.core.convert.BasicJdbcConverter
import org.springframework.data.jdbc.core.convert.DefaultJdbcTypeFactory
import org.springframework.data.jdbc.core.convert.JdbcArrayColumns
import org.springframework.data.jdbc.core.convert.JdbcConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory
import org.springframework.data.jdbc.core.convert.RelationResolver
import org.springframework.data.jdbc.core.dialect.JdbcDialect
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.relational.core.dialect.Dialect
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty
import org.springframework.data.relational.core.sql.IdentifierProcessing
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.JDBCType
import java.sql.SQLType
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration : AbstractJdbcConfiguration() {

    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                StringTilPropertiesWrapperConverter(),
                PropertiesWrapperTilStringConverter(),
                IntTilPrioritetConverter(),
                PrioritetTilIntConverter(),
            ),
        )
    }

    @Bean
    override fun jdbcConverter(
        mappingContext: JdbcMappingContext,
        operations: NamedParameterJdbcOperations,
        @Lazy relationResolver: RelationResolver,
        conversions: JdbcCustomConversions,
        dialect: Dialect,
    ): JdbcConverter {
        val arrayColumns =
            if (dialect is JdbcDialect) dialect.arraySupport else JdbcArrayColumns.DefaultSupport.INSTANCE
        val jdbcTypeFactory = DefaultJdbcTypeFactory(operations.jdbcOperations, arrayColumns)

        return CustomJdbcConverter(
            mappingContext,
            relationResolver,
            conversions,
            jdbcTypeFactory,
            dialect.identifierProcessing,
        )
    }
}

class CustomJdbcConverter(
    context: MappingContext<out RelationalPersistentEntity<*>?, out RelationalPersistentProperty?>?,
    relationResolver: RelationResolver?,
    conversions: CustomConversions?,
    typeFactory: JdbcTypeFactory?,
    identifierProcessing: IdentifierProcessing?,
) : BasicJdbcConverter(context, relationResolver, conversions, typeFactory, identifierProcessing) {

    override fun getTargetSqlType(property: RelationalPersistentProperty): SQLType {
        return if (Prioritet::class.java == property.actualType) {
            JDBCType.SMALLINT
        } else {
            super.getTargetSqlType(property)
        }
    }

    override fun getColumnType(property: RelationalPersistentProperty): Class<*> {
        return if (Prioritet::class.java == property.actualType) {
            Int::class.java
        } else {
            super.getColumnType(property)
        }
    }
}
