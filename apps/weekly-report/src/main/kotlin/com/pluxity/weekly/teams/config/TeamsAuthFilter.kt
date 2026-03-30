package com.pluxity.weekly.teams.config

import com.pluxity.common.auth.authentication.security.CustomUserDetails
import com.pluxity.common.auth.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream

private val log = KotlinLogging.logger {}

/**
 * Teams Activity body의 from.name으로 사용자를 조회하여 SecurityContext를 생성하는 필터.
 * /api/messages 요청에만 적용된다.
 */
@Component
class TeamsAuthFilter(
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.endsWith("/teams/messages")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // TODO: Azure 배포 시 Microsoft JWT 검증 추가
        //  1. Authorization 헤더에서 Bearer 토큰 추출
        //  2. Microsoft 공개키(https://login.botframework.com/v1/.well-known/openidconfiguration)로 서명 검증
        //  3. JWT의 aud 클레임 == TeamsProperties.appId 확인
        //  4. 실패 시 401 응답 반환
        val body = request.inputStream.readAllBytes()

        val name =
            try {
                val node: JsonNode = objectMapper.readTree(body)
                node.path("from").path("name").asString()
            } catch (_: Exception) {
                null
            }

        if (!name.isNullOrBlank()) {
            val user = userRepository.findByName(name)
            if (user != null) {
                val userDetails = CustomUserDetails(user)
                val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                SecurityContextHolder.getContext().authentication = auth
            } else {
                log.warn { "Teams 인증 실패 - 등록되지 않은 사용자: $name" }
            }
        } else {
            log.warn { "Teams 인증 실패 - 사용자 정보(from.name) 없음" }
        }

        filterChain.doFilter(CachedBodyRequestWrapper(request, body), response)
    }

    /**
     * HTTP body를 byte[]로 캐싱하여 InputStream을 재사용 가능하게 하는 래퍼.
     *
     * HTTP body(InputStream)는 한 번 읽으면 소비되어 재사용이 불가능하다.
     * Teams 요청은 JWT가 아닌 body(Activity JSON)의 from.name으로 사용자를 식별하므로
     * 필터에서 body를 먼저 읽어야 하고, Controller(@RequestBody)에서도 읽을 수 있도록 캐싱한다.
     */
    private class CachedBodyRequestWrapper(
        request: HttpServletRequest,
        private val cachedBody: ByteArray,
    ) : HttpServletRequestWrapper(request) {
        override fun getInputStream(): ServletInputStream {
            val byteArrayInputStream = ByteArrayInputStream(cachedBody)
            return object : ServletInputStream() {
                override fun read(): Int = byteArrayInputStream.read()

                override fun isFinished(): Boolean = byteArrayInputStream.available() == 0

                override fun isReady(): Boolean = true

                override fun setReadListener(listener: ReadListener?) {}
            }
        }
    }
}
