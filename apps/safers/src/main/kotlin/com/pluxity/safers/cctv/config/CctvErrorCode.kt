package com.pluxity.safers.cctv.config

import com.pluxity.common.core.constant.Code
import org.springframework.http.HttpStatus

enum class CctvErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_CCTV(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV를 찾을 수 없습니다."),
    NOT_FOUND_SITE(HttpStatus.NOT_FOUND, "ID가 %s인 현장을 찾을 수 없습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
