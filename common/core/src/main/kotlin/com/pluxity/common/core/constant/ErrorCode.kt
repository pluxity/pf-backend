package com.pluxity.common.core.constant

import org.springframework.http.HttpStatus

enum class ErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    // ── Auth ──
    INVALID_ID_OR_PASSWORD(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 틀렸습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ACCESS 토큰이 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "REFRESH 토큰이 유효하지 않습니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ACCESS 토큰이 만료되었습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "REFRESH 토큰이 만료되었습니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // ── User ──
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "%s는 이미 존재 하는 아이디 입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "%s 을(를) 가진 회원을 찾을 수 없습니다."),
    NOT_FOUND_ROLE(HttpStatus.NOT_FOUND, "ID가 %s인 Role을 찾을 수 없습니다."),

    // ── Permission ──
    DUPLICATE_PERMISSION_NAME(HttpStatus.BAD_REQUEST, "이름이 %s인 권한이 이미 존재합니다."),
    NOT_FOUND_PERMISSION(HttpStatus.NOT_FOUND, "ID가 %s인 Permission을 찾을 수 없습니다."),
    INVALID_RESOURCE_TYPE(HttpStatus.BAD_REQUEST, "%s는 유효하지 않은 RESOURCE TYPE 입니다."),

    // ── File ──
    INVALID_FILE_STATUS(HttpStatus.BAD_REQUEST, "적절하지 않은 파일 상태입니다."),
    FAILED_TO_ZIP_FILE(HttpStatus.BAD_REQUEST, "파일 압축에 실패했습니다."),
    FAILED_TO_UPLOAD_FILE(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    NOT_FOUND_FILE(HttpStatus.NOT_FOUND, "ID가 %s인 파일을 찾을 수 없습니다."),

    // ── Common ──
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    FAILED_TO_SAVE_ENTITY(HttpStatus.INTERNAL_SERVER_ERROR, "엔티티 저장에 실패했습니다."),
    DUPLICATE_RESOURCE_ID(HttpStatus.BAD_REQUEST, "중복된 리소스 ID가 포함되어 있습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
