package com.pluxity.yonginplatform.safetyequipment.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import com.pluxity.yonginplatform.safetyequipment.dto.SafetyEquipmentRequest
import com.pluxity.yonginplatform.safetyequipment.dto.SafetyEquipmentResponse
import com.pluxity.yonginplatform.safetyequipment.dto.toResponse
import com.pluxity.yonginplatform.safetyequipment.entity.SafetyEquipment
import com.pluxity.yonginplatform.safetyequipment.repository.SafetyEquipmentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SafetyEquipmentService(
    private val safetyEquipmentRepository: SafetyEquipmentRepository,
) {
    @Transactional
    fun create(request: SafetyEquipmentRequest): Long {
        val safetyEquipment =
            SafetyEquipment(
                name = request.name,
                quantity = request.quantity,
            )

        val saved = safetyEquipmentRepository.save(safetyEquipment)
        return saved.requiredId
    }

    fun findAll(): List<SafetyEquipmentResponse> = safetyEquipmentRepository.findAll().map { it.toResponse() }

    fun findById(id: Long): SafetyEquipmentResponse = getById(id).toResponse()

    @Transactional
    fun update(
        id: Long,
        request: SafetyEquipmentRequest,
    ) {
        val safetyEquipment = getById(id)
        safetyEquipment.update(
            name = request.name,
            quantity = request.quantity,
        )
    }

    @Transactional
    fun delete(id: Long) {
        val safetyEquipment = getById(id)
        safetyEquipmentRepository.delete(safetyEquipment)
    }

    private fun getById(id: Long): SafetyEquipment =
        safetyEquipmentRepository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_SAFETY_EQUIPMENT, id)
}
