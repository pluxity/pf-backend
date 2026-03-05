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
    NOT_FOUND_PROJECT(HttpStatus.NOT_FOUND, "ID가 %s인 프로젝트를 찾을 수 없습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "ID가 %s인 사용자를 찾을 수 없습니다."),
    DUPLICATE_PROJECT_ASSIGNMENT(HttpStatus.BAD_REQUEST, "사용자 %s은(는) 이미 프로젝트 %s에 배정되어 있습니다."),
    NOT_FOUND_PROJECT_ASSIGNMENT(HttpStatus.NOT_FOUND, "프로젝트 %s에 사용자 %s이(가) 배정되어 있지 않습니다."),
    NOT_FOUND_EPIC(HttpStatus.NOT_FOUND, "ID가 %s인 에픽을 찾을 수 없습니다."),
    DUPLICATE_EPIC_ASSIGNMENT(HttpStatus.BAD_REQUEST, "사용자 %s은(는) 이미 에픽 %s에 배정되어 있습니다."),
    NOT_FOUND_EPIC_ASSIGNMENT(HttpStatus.NOT_FOUND, "에픽 %s에 사용자 %s이(가) 배정되어 있지 않습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
