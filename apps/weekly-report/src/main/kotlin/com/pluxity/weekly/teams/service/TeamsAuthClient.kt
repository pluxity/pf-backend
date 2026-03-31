package com.pluxity.weekly.teams.service

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import com.pluxity.weekly.teams.config.TeamsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

private val log = KotlinLogging.logger {}
private const val OPENID_METADATA_URL = "https://login.botframework.com/v1/.well-known/openidconfiguration"

@Component
class TeamsAuthClient(
    webClientBuilder: WebClient.Builder,
    private val teamsProperties: TeamsProperties,
) {
    private val webClient = webClientBuilder.build()

    // Graph API 사용자 조회
    fun getGraphUser(aadObjectId: String): GraphUser? {
        return try {
            val token = fetchToken("https://graph.microsoft.com/.default")
            val response =
                webClient
                    .get()
                    .uri("https://graph.microsoft.com/v1.0/users/$aadObjectId")
                    .header("Authorization", "Bearer $token")
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .block()

            if (response == null) {
                log.warn { "Graph API 응답 없음 - aadObjectId: $aadObjectId" }
                return null
            }

            GraphUser(
                id = response["id"] as? String ?: "",
                displayName = response["displayName"] as? String,
                mail = response["mail"] as? String,
                userPrincipalName = response["userPrincipalName"] as? String,
                jobTitle = response["jobTitle"] as? String,
                department = response["department"] as? String,
            )
        } catch (e: Exception) {
            log.error(e) { "Graph API 사용자 조회 실패 - aadObjectId: $aadObjectId" }
            null
        }
    }

    // Bot Framework 토큰 발급 (봇 → Teams API 호출용)
    fun getBotToken(): String = fetchToken("https://api.botframework.com/.default")

    // Azure → PMS 요청 JWT 검증
    fun verifyBotToken(authHeader: String): Boolean {
        val token = authHeader.removePrefix("Bearer ").trim()
        if (token.isBlank()) return false

        return try {
            val jwt = SignedJWT.parse(token)
            val kid = jwt.header.keyID
            val jwksUri = fetchJwksUri()
            val jwkSet = JWKSet.load(URI.create(jwksUri).toURL())
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
            if (issuer == null || !issuer.contains("https://api.botframework.com")) {
                log.warn { "JWT issuer 불일치 - actual: $issuer" }
                return false
            }
            true
        } catch (e: Exception) {
            log.warn(e) { "JWT 검증 실패" }
            false
        }
    }

    private fun fetchToken(scope: String): String {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "client_credentials")
                add("client_id", teamsProperties.appId)
                add("client_secret", teamsProperties.appPassword)
                add("scope", scope)
            }

        val response =
            webClient
                .post()
                .uri("https://login.microsoftonline.com/${teamsProperties.tenantId}/oauth2/v2.0/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()

        val body = response ?: throw IllegalStateException("토큰 발급 실패")
        return body["access_token"] as? String
            ?: throw IllegalStateException("토큰 응답에 access_token 없음")
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
}

data class GraphUser(
    val id: String,
    val displayName: String?,
    val mail: String?,
    val userPrincipalName: String?,
    val jobTitle: String?,
    val department: String?,
)
