package no.nav.familie.prosessering.domene

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.util.TaskPrioritet.gjenbrukTaskPrioritetEller0
import org.slf4j.MDC
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.Properties

@Table("task")
data class Task(
    @Id
    val id: Long = 0L,
    val payload: String,
    val status: Status = Status.UBEHANDLET,
    val avvikstype: Avvikstype? = null,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    val triggerTid: LocalDateTime = LocalDateTime.now(),
    val type: String,
    @Column("metadata")
    val metadataWrapper: PropertiesWrapper = PropertiesWrapper(
        Properties().apply {
            this[MDCConstants.MDC_CALL_ID] =
                MDC.get(MDCConstants.MDC_CALL_ID)
                    ?: IdUtils.generateId()
        },
    ),
    @Version
    val versjon: Long = 0,
    val prioritet: Prioritet = gjenbrukTaskPrioritetEller0(),
) {

    @Transient
    val metadata: Properties = metadataWrapper.properties

    val callId: String
        get() = metadata.getProperty(MDCConstants.MDC_CALL_ID)

    constructor (
        type: String,
        payload: String,
        properties: Properties = Properties(),
        prioritet: Prioritet = gjenbrukTaskPrioritetEller0(),
    ) :
        this(
            type = type,
            payload = payload,
            metadataWrapper = PropertiesWrapper(
                properties.apply {
                    this[MDCConstants.MDC_CALL_ID] =
                        MDC.get(MDCConstants.MDC_CALL_ID)
                            ?: IdUtils.generateId()
                },
            ),
            prioritet = prioritet,
        )

    fun medTriggerTid(triggerTid: LocalDateTime): Task {
        return this.copy(triggerTid = triggerTid)
    }

    override fun toString(): String {
        return "Task(id=$id, status=$status, opprettetTid=$opprettetTid, triggerTid=$triggerTid, type='$type', versjon=$versjon, prioritet=$prioritet)"
    }
}
