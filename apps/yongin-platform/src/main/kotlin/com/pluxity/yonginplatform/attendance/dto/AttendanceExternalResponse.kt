package com.pluxity.yonginplatform.attendance.dto

data class AttendanceExternalResponse(
    val data: List<AttendanceExternalData>,
)

data class AttendanceExternalData(
    val deviceName: String,
    val attendanceCount: Int,
)
