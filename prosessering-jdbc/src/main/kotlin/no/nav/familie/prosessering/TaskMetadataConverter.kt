package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.asProperties
import no.nav.familie.prosessering.domene.asString
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class StringTilPropertiesWrapperConverter : Converter<String, PropertiesWrapper> {

    override fun convert(p0: String): PropertiesWrapper {
        return PropertiesWrapper(p0.asProperties())
    }
}

@WritingConverter
class PropertiesWrapperTilStringConverter : Converter<PropertiesWrapper, String> {

    override fun convert(taskPropertiesWrapper: PropertiesWrapper): String {
        return taskPropertiesWrapper.properties.asString()
    }
}
