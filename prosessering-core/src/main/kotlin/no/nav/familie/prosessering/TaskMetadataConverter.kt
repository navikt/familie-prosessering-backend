package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.asProperties
import no.nav.familie.prosessering.domene.asString
import no.nav.familie.prosessering.domene.Prioritet
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

@ReadingConverter
class IntTilPrioritetConverter2 : Converter<Int, Prioritet> {

    override fun convert(verdi: Int): Prioritet {
        return Prioritet.fraVerdi(verdi)
    }
}

@WritingConverter
class PrioritetTilIntConverter : Converter<Prioritet, Int> {

    override fun convert(prioritet: Prioritet): Int {
        return prioritet.verdi
    }
}
