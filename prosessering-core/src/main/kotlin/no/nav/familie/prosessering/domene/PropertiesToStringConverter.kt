package no.nav.familie.prosessering.domene

import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*


     fun Properties.asString(): String {
        if (this.isEmpty) {
            return ""
        }
        val stringWriter = StringWriter(512)
        // custom i stedet for Properties.store slik at vi ikke får med default timestamp
        this.forEach { key, value -> stringWriter.append(key as String).append('=').append(value as String).append('\n') }
        return stringWriter.toString()
    }

    fun String.asProperties(): Properties {
        val props = Properties()
        if (this.isNotBlank()) {
            try {
                props.load(StringReader(this))
            } catch (e: IOException) {
                throw IllegalArgumentException("Kan ikke lese properties til string:$props", e) //$NON-NLS-1$
            }
        }
        return props
    }
