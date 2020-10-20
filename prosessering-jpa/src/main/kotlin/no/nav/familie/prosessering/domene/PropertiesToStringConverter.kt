package no.nav.familie.prosessering.domene

import java.util.*
import javax.persistence.AttributeConverter
import javax.persistence.Converter

/**
 * JPA konverterer for å skrive ned en key=value text til et databasefelt (output tilsvarer java.util.Properties
 * format).
 */
@Converter
class PropertiesToStringConverter : AttributeConverter<Properties, String> {

    override fun convertToDatabaseColumn(props: Properties?): String? =
            props?.asString()

    override fun convertToEntityAttribute(dbData: String?): Properties =
            dbData?.asProperties() ?: Properties()
}
