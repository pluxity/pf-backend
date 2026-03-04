package com.pluxity.yongin.attendance.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.attendance.dto.AttendanceExternalData
import com.pluxity.yongin.attendance.dto.AttendanceResponse
import com.pluxity.yongin.attendance.dto.AttendanceUpdateRequest
import com.pluxity.yongin.attendance.dto.toResponse
import com.pluxity.yongin.attendance.entity.Attendance
import com.pluxity.yongin.attendance.repository.AttendanceRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val repository: AttendanceRepository,
) {
    @Transactional
    fun syncAndFindAll(
        externalData: List<AttendanceExternalData>,
        request: PageSearchRequest,
    ): PageResponse<AttendanceResponse> {
        syncAttendanceData(externalData)

        val pageable = PageRequest.of(request.page - 1, request.size)
        val page =
            repository.findPageNotNull(pageable) {
                select(entity(Attendance::class))
                    .from(entity(Attendance::class))
                    .orderBy(
                        path(Attendance::attendanceDate).desc(),
                        path(Attendance::id).asc(),
                    )
            }
        return page.toPageResponse { it.toResponse() }
    }

    fun findLatest(): List<AttendanceResponse> = repository.findAllByLatestDate().map { it.toResponse() }

    @Transactional
    fun updateWorkContent(
        id: Long,
        request: AttendanceUpdateRequest,
    ) = getById(id).updateWorkContent(request.workContent)

    private fun syncAttendanceData(externalData: List<AttendanceExternalData>) {
        if (externalData.isEmpty()) return

        val date = LocalDate.now()
        val deviceNames = externalData.map { it.deviceName }
        val existingMap =
            repository
                .findByAttendanceDateAndDeviceNameIn(date, deviceNames)
                .associateBy { it.deviceName }

        val toSave =
            externalData.map { data ->
                existingMap[data.deviceName]?.apply {
                    updateAttendanceCount(data.attendanceCount)
                } ?: Attendance(
                    attendanceDate = date,
                    deviceName = data.deviceName,
                    attendanceCount = data.attendanceCount,
                )
            }

        repository.saveAll(toSave)
    }

    private fun getById(id: Long): Attendance =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_ATTENDANCE, id)
}
