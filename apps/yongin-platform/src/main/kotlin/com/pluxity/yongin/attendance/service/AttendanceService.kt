package com.pluxity.yongin.attendance.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.attendance.client.AttendanceApiClient
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
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@Service
class AttendanceService(
    private val repository: AttendanceRepository,
    private val apiClient: AttendanceApiClient,
    private val transactionTemplate: TransactionTemplate,
) {
    fun findAllWithSync(request: PageSearchRequest): PageResponse<AttendanceResponse> {
        // 외부 API 호출 — 트랜잭션 밖에서 수행
        val externalData = apiClient.fetchAttendanceData()

        // DB 동기화 + 조회 — 트랜잭션 안에서 수행
        return transactionTemplate.execute {
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
            page.toPageResponse { it.toResponse() }
        }
    }

    @Transactional(readOnly = true)
    fun findLatest(): List<AttendanceResponse> = repository.findAllByLatestDate().map { it.toResponse() }

    @Transactional
    fun updateWorkContent(
        id: Long,
        request: AttendanceUpdateRequest,
    ) = getById(id).updateWorkContent(request.workContent)

    private fun syncAttendanceData(externalData: List<AttendanceExternalData>) {
        if (externalData.isEmpty()) return

        val date = LocalDate.now()

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
