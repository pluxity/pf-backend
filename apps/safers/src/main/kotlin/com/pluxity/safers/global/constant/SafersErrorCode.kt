package com.pluxity.safers.global.constant

import com.pluxity.common.core.constant.Code
import org.springframework.http.HttpStatus

enum class SafersErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_EVENT(HttpStatus.NOT_FOUND, "ID가 %s인 이벤트를 찾을 수 없습니다."),
    NOT_FOUND_SITE(HttpStatus.NOT_FOUND, "ID가 %s인 현장을 찾을 수 없습니다."),
    NOT_FOUND_CONFIGURATION(HttpStatus.NOT_FOUND, "키가 %s인 설정을 찾을 수 없습니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "위치 정보는 Polygon 형식이어야 합니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
