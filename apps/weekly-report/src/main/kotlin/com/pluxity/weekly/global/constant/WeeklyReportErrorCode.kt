package com.pluxity.weekly.global.constant

import com.pluxity.common.core.constant.Code
import org.springframework.http.HttpStatus

enum class WeeklyReportErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_TEAM(HttpStatus.NOT_FOUND, "ID가 %s인 팀을 찾을 수 없습니다."),
    NOT_FOUND_TEAM_MEMBER(HttpStatus.NOT_FOUND, "팀 %s에 사용자 %s이(가) 소속되어 있지 않습니다."),
    DUPLICATE_TEAM_MEMBER(HttpStatus.BAD_REQUEST, "사용자 %s은(는) 이미 팀 %s에 소속되어 있습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
