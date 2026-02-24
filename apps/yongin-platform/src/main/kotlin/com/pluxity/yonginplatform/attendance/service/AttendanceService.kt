package com.pluxity.yonginplatform.attendance.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yonginplatform.attendance.client.AttendanceApiClient
import com.pluxity.yonginplatform.attendance.dto.AttendanceResponse
import com.pluxity.yonginplatform.attendance.dto.AttendanceUpdateRequest
import com.pluxity.yonginplatform.attendance.dto.toResponse
import com.pluxity.yonginplatform.attendance.entity.Attendance
import com.pluxity.yonginplatform.attendance.repository.AttendanceRepository
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val repository: AttendanceRepository,
    private val apiClient: AttendanceApiClient,
) {
    @Transactional
    fun findAllWithSync(request: PageSearchRequest): PageResponse<AttendanceResponse> {
        // 외부 API 호출하여 데이터 동기화
        syncAttendanceData()

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

    private fun syncAttendanceData() {
        val date = LocalDate.now()
        val externalData = apiClient.fetchAttendanceData()

        if (externalData.isEmpty()) return

        // 외부 데이터의 deviceName 목록 추출
        val deviceNames = externalData.map { it.deviceName }

        // 날짜+deviceName 조건으로 한 번에 조회 후 Map으로 변환
        val existingMap =
            repository
                .findByAttendanceDateAndDeviceNameIn(date, deviceNames)
                .associateBy { it.deviceName }

        // 수정/신규 목록 생성
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

        // 한 번에 저장
        repository.saveAll(toSave)
    }

    private fun getById(id: Long): Attendance =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_ATTENDANCE, id)
}
