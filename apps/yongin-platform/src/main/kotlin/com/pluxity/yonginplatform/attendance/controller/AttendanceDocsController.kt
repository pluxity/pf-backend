package com.pluxity.yonginplatform.attendance.controller

import com.pluxity.yonginplatform.attendance.dto.AttendanceExternalData
import com.pluxity.yonginplatform.attendance.dto.AttendanceExternalResponse
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
@RequestMapping("/docs/attendance")
class AttendanceDocsController {
    @GetMapping
    fun getAttendanceExternalResponse(): ResponseEntity<AttendanceExternalResponse> =
        ResponseEntity.ok(
            AttendanceExternalResponse(
                data =
                    listOf(
                        AttendanceExternalData(
                            deviceName = "근로자휴게실",
                            attendanceCount = 10,
                        ),
                        AttendanceExternalData(
                            deviceName = "단말기1",
                            attendanceCount = 15,
                        ),
                        AttendanceExternalData(
                            deviceName = "단말기2",
                            attendanceCount = 20,
                        ),
                    ),
            ),
        )
}
