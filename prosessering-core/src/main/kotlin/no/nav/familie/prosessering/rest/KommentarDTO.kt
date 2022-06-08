package no.nav.familie.prosessering.rest


import no.nav.familie.prosessering.domene.Status

data class KommentarDTO(val settTilManuellOppfølging: Boolean,
                        val kommentar: String)
