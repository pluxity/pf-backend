package com.pluxity.common.messaging.interceptor

import com.pluxity.common.auth.authentication.security.JwtProvider
import com.pluxity.common.auth.properties.JwtProperties
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.messaging.dto.StompPrincipal
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

@Component
class MyDefaultHandshakeHandler(
    private val jwtProperties: JwtProperties,
    private val jwtProvider: JwtProvider,
) : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Principal? {
        val servletRequest = (request as ServletServerHttpRequest).servletRequest
        val token = servletRequest.cookies?.find { it.name == jwtProperties.accessToken.name }?.value
        token?.let {
            attributes["username"] = jwtProvider.extractUsername(it)
        } ?: throw CustomException(ErrorCode.NOT_FOUND_USER, attributes["username"])
        return StompPrincipal()
    }
}
