package com.pluxity.yonginplatform.attendance.dto

import com.pluxity.yonginplatform.attendance.entity.Attendance
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "출역현황 응답")
data class AttendanceResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "출역일자", example = "2024-01-15")
    val attendanceDate: LocalDate,
    @field:Schema(description = "장치명", example = "1공구 단말기")
    val deviceName: String,
    @field:Schema(description = "출역인원", example = "25")
    val attendanceCount: Int,
    @field:Schema(description = "금일작업내용", example = "콘크리트 타설 작업")
    val workContent: String?,
)

fun Attendance.toResponse() =
    AttendanceResponse(
        id = this.requiredId,
        attendanceDate = this.attendanceDate,
        deviceName = this.deviceName,
        attendanceCount = this.attendanceCount,
        workContent = this.workContent,
    )
