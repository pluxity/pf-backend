package com.pluxity.safers.cctv.config

import com.pluxity.common.core.constant.Code
import org.springframework.http.HttpStatus

enum class CctvErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_CCTV(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV를 찾을 수 없습니다."),
    NOT_FOUND_SITE(HttpStatus.NOT_FOUND, "ID가 %s인 현장을 찾을 수 없습니다."),
    MISSING_NVR_INFO(HttpStatus.BAD_REQUEST, "CCTV(ID: %s)에 NVR 정보가 설정되지 않았습니다."),
    MISSING_BASE_URL(HttpStatus.BAD_REQUEST, "현장(ID: %s)에 미디어서버 URL이 설정되지 않았습니다."),
    PLAYBACK_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "재생 요청에 대한 응답을 받지 못했습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
