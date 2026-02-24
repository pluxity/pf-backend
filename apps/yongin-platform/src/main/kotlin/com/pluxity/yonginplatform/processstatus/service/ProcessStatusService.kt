package com.pluxity.yonginplatform.processstatus.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findAllByIdsOrThrow
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import com.pluxity.yonginplatform.processstatus.dto.ProcessStatusBulkRequest
import com.pluxity.yonginplatform.processstatus.dto.ProcessStatusResponse
import com.pluxity.yonginplatform.processstatus.dto.toResponse
import com.pluxity.yonginplatform.processstatus.entity.ProcessStatus
import com.pluxity.yonginplatform.processstatus.repository.ProcessStatusRepository
import com.pluxity.yonginplatform.processstatus.repository.WorkTypeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProcessStatusService(
    private val repository: ProcessStatusRepository,
    private val workTypeRepository: WorkTypeRepository,
) {
    fun findAll(request: PageSearchRequest): PageResponse<ProcessStatusResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)

        val page =
            repository.findPageNotNull(pageable) {
                select(entity(ProcessStatus::class))
                    .from(entity(ProcessStatus::class))
                    .orderBy(path(ProcessStatus::workDate).desc())
            }
        return page.toPageResponse { it.toResponse() }
    }

    fun findLatest(): List<ProcessStatusResponse> = repository.findAllByLatestWorkDate().map { it.toResponse() }

    @Transactional
    fun saveOrUpdateAll(request: ProcessStatusBulkRequest) {
        // Delete
        if (request.deletedIds.isNotEmpty()) {
            repository.deleteAllById(request.deletedIds)
        }

        if (request.upserts.isEmpty()) return

        // WorkType 한번에 조회
        val workTypeIds = request.upserts.map { it.workTypeId }.distinct()
        val workTypeMap =
            findAllByIdsOrThrow(
                ids = workTypeIds,
                findAllById = workTypeRepository::findAllById,
                idExtractor = { it.requiredId },
                errorCode = YonginErrorCode.NOT_FOUND_WORK_TYPE,
            )

        // 수정할 ProcessStatus 한번에 조회
        val updateIds = request.upserts.mapNotNull { it.id }
        val processStatusMap =
            findAllByIdsOrThrow(
                ids = updateIds,
                findAllById = repository::findAllById,
                idExtractor = { it.requiredId },
                errorCode = YonginErrorCode.NOT_FOUND_PROCESS_STATUS,
            )

        // Upsert
        request.upserts.forEach { item ->
            val workType =
                workTypeMap[item.workTypeId]
                    ?: throw CustomException(YonginErrorCode.NOT_FOUND_WORK_TYPE, item.workTypeId)

            if (item.id == null) {
                repository.save(
                    ProcessStatus(
                        workDate = item.workDate,
                        workType = workType,
                        plannedRate = item.plannedRate,
                        actualRate = item.actualRate,
                        isActive = item.isActive,
                    ),
                )
                return@forEach
            }

            (processStatusMap[item.id] ?: throw CustomException(YonginErrorCode.NOT_FOUND_PROCESS_STATUS, item.id))
                .update(
                    workDate = item.workDate,
                    workType = workType,
                    plannedRate = item.plannedRate,
                    actualRate = item.actualRate,
                    isActive = item.isActive,
                )
        }
    }
}
