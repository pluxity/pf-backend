package com.pluxity.cctv.config

import com.pluxity.common.core.constant.Code
import org.springframework.http.HttpStatus

enum class CctvErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_CCTV(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV를 찾을 수 없습니다."),
    MEDIA_SERVER_URL_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "미디어서버 URL이 설정되지 않았습니다."),
    EXCEED_FAVORITE_LIMIT(HttpStatus.BAD_REQUEST, "즐겨찾기는 최대 %s개까지 추가할 수 있습니다."),
    ALREADY_FAVORITE(HttpStatus.BAD_REQUEST, "스트림명이 %s인 CCTV는 이미 즐겨찾기되어 있습니다."),
    INVALID_FAVORITE_ORDER_COUNT(HttpStatus.BAD_REQUEST, "즐겨찾기 순서 변경은 전체 즐겨찾기를 대상으로 해야 합니다."),
    NOT_FOUND_CCTV_FAVORITE(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV 즐겨찾기를 찾을 수 없습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
