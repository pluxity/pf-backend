package com.pluxity.yonginplatform.notice.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.yonginplatform.notice.entity.Notice
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "공지사항 응답")
data class NoticeResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "제목", example = "공지사항 제목입니다")
    val title: String,
    @field:Schema(description = "내용", example = "공지사항 내용입니다")
    val content: String?,
    @field:Schema(description = "노출 여부", example = "false")
    val isVisible: Boolean,
    @field:Schema(description = "상시 게시 여부", example = "false")
    val isAlways: Boolean,
    @field:Schema(description = "게시 시작일", example = "2026-01-01")
    val startDate: LocalDate?,
    @field:Schema(description = "게시 종료일", example = "2026-12-31")
    val endDate: LocalDate?,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun Notice.toResponse(): NoticeResponse =
    NoticeResponse(
        id = this.requiredId,
        title = this.title,
        content = this.content,
        isVisible = this.isVisible,
        isAlways = this.isAlways,
        startDate = this.startDate,
        endDate = this.endDate,
        baseResponse = this.toBaseResponse(),
    )
