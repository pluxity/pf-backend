package com.pluxity.yongin.attendance.dto

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
