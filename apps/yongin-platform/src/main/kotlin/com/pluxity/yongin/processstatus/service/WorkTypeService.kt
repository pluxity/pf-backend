package com.pluxity.yongin.processstatus.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.processstatus.dto.WorkTypeRequest
import com.pluxity.yongin.processstatus.dto.WorkTypeResponse
import com.pluxity.yongin.processstatus.dto.toResponse
import com.pluxity.yongin.processstatus.entity.WorkType
import com.pluxity.yongin.processstatus.repository.ProcessStatusRepository
import com.pluxity.yongin.processstatus.repository.WorkTypeRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WorkTypeService(
    private val repository: WorkTypeRepository,
    private val processStatusRepository: ProcessStatusRepository,
) {
    fun findAll(): List<WorkTypeResponse> = repository.findAll().map { it.toResponse() }

    @Transactional
    fun create(request: WorkTypeRequest): Long = repository.save(WorkType(name = request.name)).requiredId

    @Transactional
    fun delete(id: Long) {
        val workType = getById(id)
        if (processStatusRepository.existsByWorkType(workType)) {
            throw CustomException(YonginErrorCode.WORK_TYPE_HAS_PROCESS_STATUS)
        }
        repository.deleteById(workType.requiredId)
    }

    fun getById(id: Long) =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_WORK_TYPE, id)
}
