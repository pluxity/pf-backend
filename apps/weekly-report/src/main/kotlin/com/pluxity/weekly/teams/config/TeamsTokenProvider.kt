package com.pluxity.weekly.teams.config

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

private const val OPENID_METADATA_URL =
    "https://login.botframework.com/v1/.well-known/openidconfiguration"
private const val TOKEN_URL_TEMPLATE =
    "https://login.microsoftonline.com/%s/oauth2/v2.0/token"
private const val JWK_CACHE_TTL_MS = 24 * 60 * 60 * 1000L

@Component
class TeamsTokenProvider(
    private val teamsProperties: TeamsProperties,
    webClientBuilder: WebClient.Builder,
) {
    private val webClient = webClientBuilder.build()
    private val jwkCache = AtomicReference<CachedJwkSet>()
    private val tokenCache = AtomicReference<CachedToken>()

    // azure -> pms 요청 검증
    fun verifyTeamsToken(authHeader: String): Boolean {
        val token = authHeader.removePrefix("Bearer ").trim()
        if (token.isBlank()) return false

        return try {
            val jwt = SignedJWT.parse(token)
            val kid = jwt.header.keyID

            val jwkSet = getJwkSet()
            val jwk = jwkSet.getKeyByKeyId(kid) as? RSAKey
            if (jwk == null) {
                log.warn { "JWT kid에 매칭되는 공개키 없음: $kid" }
                return false
            }

            if (!jwt.verify(RSASSAVerifier(jwk))) {
                log.warn { "JWT 서명 검증 실패" }
                return false
            }

            val claims = jwt.jwtClaimsSet

            if (claims.audience == null || teamsProperties.appId !in claims.audience) {
                log.warn { "JWT aud 불일치 - expected: ${teamsProperties.appId}, actual: ${claims.audience}" }
                return false
            }

            val issuer = claims.issuer
            if (issuer == null || !issuer.contains("login.microsoftonline.com")) {
                log.warn { "JWT issuer 불일치 - actual: $issuer" }
                return false
            }

            true
        } catch (e: Exception) {
            log.warn(e) { "JWT 검증 실패" }
            false
        }
    }

    // 봇 서버 → Teams API 호출용 토큰 발급
    fun getTeamsToken(): String {
        val cached = tokenCache.get()
        if (cached != null && !cached.isExpired()) return cached.token

        val tenantId = teamsProperties.tenantId.ifBlank { "botframework.com" }
        val tokenUrl = TOKEN_URL_TEMPLATE.format(tenantId)

        val response =
            webClient
                .post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(
                    "grant_type=client_credentials" +
                        "&client_id=${teamsProperties.appId}" +
                        "&client_secret=${teamsProperties.appPassword}" +
                        "&scope=https://api.botframework.com/.default",
                )
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
                ?: throw IllegalStateException("Teams 토큰 발급 실패")

        val accessToken =
            response["access_token"] as? String
                ?: throw IllegalStateException("Teams 토큰 응답에 access_token 없음")
        val expiresIn =
            (response["expires_in"] as? Number)?.toLong() ?: 3600L

        tokenCache.set(CachedToken(accessToken, expiresIn))
        return accessToken
    }

    private fun getJwkSet(): JWKSet {
        val cached = jwkCache.get()
        if (cached != null && !cached.isExpired()) return cached.jwkSet

        val jwksUri = fetchJwksUri()
        val jwkSet = JWKSet.load(URI.create(jwksUri).toURL())
        jwkCache.set(CachedJwkSet(jwkSet))
        return jwkSet
    }

    private fun fetchJwksUri(): String {
        val metadata =
            webClient
                .get()
                .uri(OPENID_METADATA_URL)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
                ?: throw IllegalStateException("OpenID metadata 조회 실패")

        return metadata["jwks_uri"] as? String
            ?: throw IllegalStateException("OpenID metadata에 jwks_uri 없음")
    }

    private data class CachedJwkSet(
        val jwkSet: JWKSet,
        val cachedAt: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > JWK_CACHE_TTL_MS
    }

    private data class CachedToken(
        val token: String,
        val expiresIn: Long,
        val cachedAt: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > (expiresIn - 60) * 1000
    }
}
