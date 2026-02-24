package com.pluxity.yonginplatform.processstatus.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import com.pluxity.yonginplatform.processstatus.dto.WorkTypeRequest
import com.pluxity.yonginplatform.processstatus.dto.WorkTypeResponse
import com.pluxity.yonginplatform.processstatus.dto.toResponse
import com.pluxity.yonginplatform.processstatus.entity.WorkType
import com.pluxity.yonginplatform.processstatus.repository.ProcessStatusRepository
import com.pluxity.yonginplatform.processstatus.repository.WorkTypeRepository
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
