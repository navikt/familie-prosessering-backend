package no.nav.familie.prosessering.sikkerhet

import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

internal class ProsesseringJwtDecoder(
    private val issuerUri: String,
    private val acceptedAudience: String,
) : JwtDecoder {
    companion object {
        val AUDIENCE_CLAIM = "aud"
    }

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private val delegate: JwtDecoder by lazy {
        println("kom til dekoderen")
        val decoder = JwtDecoders.fromIssuerLocation(issuerUri) as NimbusJwtDecoder
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                JwtClaimValidator<List<String>>(AUDIENCE_CLAIM) { aud ->
                    val ok = aud.contains(acceptedAudience)
                    if (!ok) {
                        secureLogger.warn(
                            "Ugyldig JWT: uforventet audience. aud=$aud, forventet=$acceptedAudience",
                        )
                    }
                    ok
                },
            ),
        )
        decoder
    }

    override fun decode(token: String): Jwt = delegate.decode(token)
}
