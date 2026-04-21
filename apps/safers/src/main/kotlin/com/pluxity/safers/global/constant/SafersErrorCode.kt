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
    NOT_FOUND_CONFIGURATION_BY_ID(HttpStatus.NOT_FOUND, "ID가 %s인 설정을 찾을 수 없습니다."),
    DUPLICATE_CONFIGURATION(HttpStatus.CONFLICT, "키가 %s인 설정이 이미 존재합니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "위치 정보는 Polygon 형식이어야 합니다."),
    NOT_FOUND_CCTV(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV를 찾을 수 없습니다."),
    MISSING_NVR_INFO(HttpStatus.BAD_REQUEST, "CCTV(ID: %s)에 NVR 정보가 설정되지 않았습니다."),
    MISSING_BASE_URL(HttpStatus.BAD_REQUEST, "현장(ID: %s)에 미디어서버 URL이 설정되지 않았습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다. (yyyyMMddHHmmss)"),
    PLAYBACK_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "재생 요청에 대한 응답을 받지 못했습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
