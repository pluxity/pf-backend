package com.pluxity.yongin.global.constant

import com.pluxity.common.core.constant.Code
import org.springframework.http.HttpStatus

enum class YonginErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    NOT_FOUND_GOAL(HttpStatus.NOT_FOUND, "ID가 %s인 목표관리를 찾을 수 없습니다."),
    NOT_FOUND_CONSTRUCTION_SECTION(HttpStatus.NOT_FOUND, "ID가 %s인 시공구간을 찾을 수 없습니다."),
    CONSTRUCTION_SECTION_HAS_GOAL(HttpStatus.BAD_REQUEST, "시공구간에 등록된 목표관리가 있어 삭제할 수 없습니다."),
    NOT_FOUND_PROCESS_STATUS(HttpStatus.NOT_FOUND, "ID가 %s인 공정현황을 찾을 수 없습니다."),
    NOT_FOUND_WORK_TYPE(HttpStatus.NOT_FOUND, "ID가 %s인 공정명을 찾을 수 없습니다."),
    WORK_TYPE_HAS_PROCESS_STATUS(HttpStatus.BAD_REQUEST, "공정명에 등록된 공정현황이 있어 삭제할 수 없습니다."),
    NOT_FOUND_KEY_MANAGEMENT(HttpStatus.NOT_FOUND, "ID가 %s인 주요관리사항을 찾을 수 없습니다."),
    DUPLICATE_KEY_MANAGEMENT_DISPLAY_ORDER(HttpStatus.BAD_REQUEST, "타입 %s에 이미 %s번 값이 존재합니다."),
    NOT_FOUND_NOTICE(HttpStatus.NOT_FOUND, "ID가 %s인 공지사항을 찾을 수 없습니다."),
    INVALID_NOTICE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "상시 게시가 아닌 경우 시작일 또는 종료일 중 하나는 필수입니다."),
    INVALID_NOTICE_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일이 종료일보다 이후일 수 없습니다."),
    NOT_FOUND_ATTENDANCE(HttpStatus.NOT_FOUND, "ID가 %s인 출역현황을 찾을 수 없습니다."),
    NOT_FOUND_OBSERVATION(HttpStatus.NOT_FOUND, "ID가 %s인 드론 관측 데이터를 찾을 수 없습니다."),
    NOT_FOUND_SAFETY_EQUIPMENT(HttpStatus.NOT_FOUND, "ID가 %s인 안전장비를 찾을 수 없습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
