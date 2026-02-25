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
    NOT_FOUND_CCTV(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV를 찾을 수 없습니다."),
    MEDIA_SERVER_URL_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "미디어서버 URL이 설정되지 않았습니다."),
    EXCEED_FAVORITE_LIMIT(HttpStatus.BAD_REQUEST, "즐겨찾기는 최대 4개까지 추가할 수 있습니다."),
    ALREADY_FAVORITE(HttpStatus.BAD_REQUEST, "스트림명이 %s인 CCTV는 이미 즐겨찾기되어 있습니다."),
    INVALID_FAVORITE_ORDER_COUNT(HttpStatus.BAD_REQUEST, "즐겨찾기 순서 변경은 전체 즐겨찾기를 대상으로 해야 합니다."),
    NOT_FOUND_CCTV_FAVORITE(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV 즐겨찾기를 찾을 수 없습니다."),
    NOT_FOUND_SAFETY_EQUIPMENT(HttpStatus.NOT_FOUND, "ID가 %s인 안전장비를 찾을 수 없습니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name

    override fun getCodeName(): String = name
}
