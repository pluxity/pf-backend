package com.pluxity.yongin.attendance.dto

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.test.withId
import com.pluxity.yongin.attendance.entity.Attendance
import java.time.LocalDate

fun dummyAttendance(
    id: Long = 1L,
    attendanceDate: LocalDate = LocalDate.now(),
    deviceName: String = "테스트 단말기",
    attendanceCount: Int = 10,
    workContent: String? = null,
): Attendance =
    Attendance(
        attendanceDate = attendanceDate,
        deviceName = deviceName,
        attendanceCount = attendanceCount,
        workContent = workContent,
    ).withId(id)

fun dummyAttendanceResponse(
    id: Long = 1L,
    attendanceDate: LocalDate = LocalDate.of(2026, 1, 15),
    deviceName: String = "입구 게이트",
    attendanceCount: Int = 50,
    workContent: String? = "콘크리트 타설 작업",
) = AttendanceResponse(
    id = id,
    attendanceDate = attendanceDate,
    deviceName = deviceName,
    attendanceCount = attendanceCount,
    workContent = workContent,
)

fun dummyAttendanceUpdateRequest(workContent: String = "콘크리트 타설 작업") =
    AttendanceUpdateRequest(
        workContent = workContent,
    )

fun dummyPageSearchRequest(
    page: Int = 1,
    size: Int = 10,
) = PageSearchRequest(
    page = page,
    size = size,
)
