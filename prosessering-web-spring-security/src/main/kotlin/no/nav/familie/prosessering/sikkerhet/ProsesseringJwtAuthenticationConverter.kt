package no.nav.familie.prosessering.sikkerhet

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component("prosesseringJwtAuthenticationConverter")
class ProsesseringJwtAuthenticationConverter(
    @param:Value("\${AZURE_OPENID_CONFIG_ISSUER}") val issuerUri: String,
    @param:Value("\${AZURE_APP_CLIENT_ID}") private val acceptedAudience: String,
) : Converter<Jwt, AbstractAuthenticationToken> {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val issuer = jwt.getClaimAsString("iss")
        if (issuer == null) {
            secureLogger.warn("Ugyldig JWT: mangler 'iss' claim")
            throw invalidToken("JWT mangler 'iss' claim")
        }
        if (issuer != issuerUri) {
            secureLogger.warn("Ugyldig JWT: uforventet issuer. iss=$issuer, forventet=$issuerUri")
            throw invalidToken("Ugyldig issuer")
        }

        val aud = jwt.getClaimAsStringList("aud")
        if (aud.isNullOrEmpty()) {
            secureLogger.warn("Ugyldig JWT: mangler 'aud' claim")
            throw invalidToken("JWT mangler 'aud' claim")
        }
        if (!aud.contains(acceptedAudience)) {
            secureLogger.warn("Ugyldig JWT: uforventet audience. aud=$aud, forventet=$acceptedAudience")
            throw invalidToken("Ugyldig audience")
        }

        return JwtAuthenticationToken(jwt)
    }

    private fun invalidToken(message: String): OAuth2AuthenticationException =
        OAuth2AuthenticationException(OAuth2Error("invalid_token", message, null))
}
