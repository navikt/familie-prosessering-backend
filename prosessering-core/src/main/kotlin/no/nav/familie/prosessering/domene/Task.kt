package no.nav.familie.prosessering.domene

import no.nav.familie.prosessering.util.IdUtils
import no.nav.familie.prosessering.util.MDCConstants
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
    val metadataWrapper: PropertiesWrapper =
        PropertiesWrapper(
            Properties().apply {
                this[MDCConstants.MDC_CALL_ID] =
                    MDC.get(MDCConstants.MDC_CALL_ID)
                        ?: IdUtils.generateId()
                this[MDCConstants.MDC_FAGSAK_ID] =
                    MDC.get(MDCConstants.MDC_FAGSAK_ID)
                        ?: "IKKE_SATT"
                this[MDCConstants.MDC_BEHANDLING_ID] =
                    MDC.get(MDCConstants.MDC_BEHANDLING_ID)
                        ?: "IKKE_SATT"
            },
        ),
    @Version
    val versjon: Long = 0,
) {
    @Transient
    val metadata: Properties = metadataWrapper.properties

    val callId: String
        get() = metadata.getProperty(MDCConstants.MDC_CALL_ID)
    val fagsakId: String?
        get() = metadata.getProperty(MDCConstants.MDC_FAGSAK_ID)
    val behandlingId: String?
        get() = metadata.getProperty(MDCConstants.MDC_BEHANDLING_ID)

    constructor (type: String, payload: String, properties: Properties = Properties()) :
        this(
            type = type,
            payload = payload,
            metadataWrapper =
                PropertiesWrapper(
                    properties.apply {
                        this[MDCConstants.MDC_CALL_ID] =
                            MDC.get(MDCConstants.MDC_CALL_ID)
                                ?: IdUtils.generateId()
                        if (!MDC.get(MDCConstants.MDC_FAGSAK_ID).isNullOrEmpty()) {
                            this[MDCConstants.MDC_FAGSAK_ID] = MDC.get(MDCConstants.MDC_FAGSAK_ID)
                        }
                        if (!MDC.get(MDCConstants.MDC_BEHANDLING_ID).isNullOrEmpty()) {
                            this[MDCConstants.MDC_BEHANDLING_ID] = MDC.get(MDCConstants.MDC_BEHANDLING_ID)
                        }
                    },
                ),
        )

    fun medTriggerTid(triggerTid: LocalDateTime): Task = this.copy(triggerTid = triggerTid)

    override fun toString(): String =
        "Task(id=$id, status=$status, opprettetTid=$opprettetTid, fagsakId=$fagsakId, behandlingId=$behandlingId, triggerTid=$triggerTid, type='$type', versjon=$versjon)"
}
