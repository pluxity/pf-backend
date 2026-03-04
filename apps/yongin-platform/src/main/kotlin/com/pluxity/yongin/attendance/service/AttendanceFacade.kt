package com.pluxity.yongin.attendance.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.PageResponse
import com.pluxity.yongin.attendance.client.AttendanceApiClient
import com.pluxity.yongin.attendance.dto.AttendanceResponse
import com.pluxity.yongin.attendance.dto.AttendanceUpdateRequest
import org.springframework.stereotype.Component

@Component
class AttendanceFacade(
    private val attendanceService: AttendanceService,
    private val apiClient: AttendanceApiClient,
) {
    fun findAllWithSync(request: PageSearchRequest): PageResponse<AttendanceResponse> {
        val externalData = apiClient.fetchAttendanceData()
        return attendanceService.syncAndFindAll(externalData, request)
    }

    fun findLatest(): List<AttendanceResponse> = attendanceService.findLatest()

    fun updateWorkContent(
        id: Long,
        request: AttendanceUpdateRequest,
    ) = attendanceService.updateWorkContent(id, request)
}
