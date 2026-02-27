package com.pluxity.common.auth.authentication.security

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.pluxity.common.auth.authentication.repository.RefreshTokenRepository
import com.pluxity.common.auth.properties.JwtProperties
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.web.util.WebUtils
import java.text.ParseException
import java.util.Base64
import java.util.Date

@Service
class JwtProvider(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
) {
    fun extractUsername(
        token: String,
        isRefreshToken: Boolean = false,
    ): String {
        val claimsSet = extractAllClaims(token, isRefreshToken)
        return claimsSet.subject
    }

    private fun extractAllClaims(
        token: String,
        isRefreshToken: Boolean,
    ): JWTClaimsSet {
        val signedJWT = SignedJWT.parse(token)
        val verifier = MACVerifier(getSecretKeyBytes(isRefreshToken))
        if (!signedJWT.verify(verifier)) {
            throw CustomException(
                if (isRefreshToken) ErrorCode.INVALID_REFRESH_TOKEN else ErrorCode.INVALID_ACCESS_TOKEN,
            )
        }
        return signedJWT.jwtClaimsSet
    }

    fun generateAccessToken(
        username: String,
        extraClaims: Map<String, Any> = emptyMap(),
    ): String = buildToken(extraClaims, username, jwtProperties.accessToken.expiration, false)

    fun generateRefreshToken(username: String): String = buildToken(emptyMap(), username, jwtProperties.refreshToken.expiration, true)

    private fun buildToken(
        extraClaims: Map<String, Any>,
        username: String,
        expiration: Long,
        isRefreshToken: Boolean,
    ): String {
        val now = System.currentTimeMillis()
        val claimsBuilder =
            JWTClaimsSet
                .Builder()
                .subject(username)
                .issueTime(Date(now))
                .expirationTime(Date(now + expiration * 1000))

        extraClaims.forEach { (key, value) -> claimsBuilder.claim(key, value) }

        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build())
        signedJWT.sign(MACSigner(getSecretKeyBytes(isRefreshToken)))
        return signedJWT.serialize()
    }

    fun isAccessTokenValid(token: String): Boolean =
        runCatching {
            val signedJWT = SignedJWT.parse(token)
            val verifier = MACVerifier(getSecretKeyBytes(false))
            if (!signedJWT.verify(verifier)) {
                throw CustomException(ErrorCode.INVALID_ACCESS_TOKEN)
            }
            val expirationTime = signedJWT.jwtClaimsSet.expirationTime
            if (expirationTime != null && expirationTime.before(Date())) {
                throw CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN)
            }
        }.fold(
            onSuccess = { true },
            onFailure = { exception ->
                when (exception) {
                    is CustomException -> throw exception
                    is ParseException, is JOSEException, is IllegalArgumentException -> {
                        throw CustomException(ErrorCode.INVALID_ACCESS_TOKEN)
                    }
                    else -> throw exception
                }
            },
        )

    fun isRefreshTokenValid(token: String?): Boolean {
        if (token.isNullOrBlank()) return false

        return runCatching {
            val refreshToken =
                refreshTokenRepository
                    .findByToken(token)
                    ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

            if (!refreshToken.isValidToken()) {
                throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
            }

            val signedJWT = SignedJWT.parse(refreshToken.token)
            val verifier = MACVerifier(getSecretKeyBytes(true))
            if (!signedJWT.verify(verifier)) {
                throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
            }
            val expirationTime = signedJWT.jwtClaimsSet.expirationTime
            if (expirationTime != null && expirationTime.before(Date())) {
                throw CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN)
            }
        }.fold(
            onSuccess = { true },
            onFailure = { exception ->
                when (exception) {
                    is CustomException -> throw exception
                    is ParseException, is JOSEException, is IllegalArgumentException -> {
                        throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
                    }
                    else -> throw exception
                }
            },
        )
    }

    private fun getSecretKeyBytes(isRefreshToken: Boolean): ByteArray {
        val key = if (isRefreshToken) jwtProperties.refreshToken.secretKey else jwtProperties.accessToken.secretKey
        return Base64.getDecoder().decode(key)
    }

    fun getAccessTokenFromRequest(request: HttpServletRequest): String? = getJwtFromRequest(jwtProperties.accessToken.name, request)

    fun getJwtFromRequest(
        name: String,
        request: HttpServletRequest,
    ): String? = WebUtils.getCookie(request, name)?.value
}
