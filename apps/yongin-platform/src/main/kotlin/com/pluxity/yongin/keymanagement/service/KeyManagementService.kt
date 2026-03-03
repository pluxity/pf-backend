package com.pluxity.yongin.keymanagement.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.extensions.getFileMapById
import com.pluxity.common.file.service.FileService
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.keymanagement.dto.KeyManagementGroupResponse
import com.pluxity.yongin.keymanagement.dto.KeyManagementRequest
import com.pluxity.yongin.keymanagement.dto.KeyManagementResponse
import com.pluxity.yongin.keymanagement.dto.KeyManagementUpdateRequest
import com.pluxity.yongin.keymanagement.dto.toResponse
import com.pluxity.yongin.keymanagement.entity.KeyManagement
import com.pluxity.yongin.keymanagement.entity.KeyManagementType
import com.pluxity.yongin.keymanagement.repository.KeyManagementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class KeyManagementService(
    private val repository: KeyManagementRepository,
    private val fileService: FileService,
) {
    companion object {
        private const val KEY_MANAGEMENT: String = "key-management/"
    }

    fun findAll(): List<KeyManagementGroupResponse> {
        val keyManagements = repository.findAll()

        val fileMap = fileService.getFileMapById(keyManagements) { it.fileId }

        return KeyManagementType.sortedEntries().map { type ->
            KeyManagementGroupResponse(
                type = type,
                typeDescription = type.description,
                items =
                    keyManagements
                        .filter { it.type == type }
                        .sortedBy { it.displayOrder }
                        .map { it.toResponse(it.fileId?.let { id -> fileMap[id] }) },
            )
        }
    }

    fun findSelected(): List<KeyManagementResponse> {
        val keyManagements = repository.findBySelectedTrue()

        val fileMap = fileService.getFileMapById(keyManagements) { it.fileId }

        return keyManagements
            .sortedWith(compareBy({ it.type.order }, { it.displayOrder }))
            .map { it.toResponse(it.fileId?.let { id -> fileMap[id] }) }
    }

    fun findById(id: Long): KeyManagementResponse {
        val keyManagement = getById(id)
        return keyManagement.toResponse(keyManagement.fileId?.let { fileService.getFileResponse(it) })
    }

    @Transactional
    fun create(request: KeyManagementRequest): Long {
        validateDisplayOrderUnique(request.type, request.displayOrder)
        val savedKeyManagement =
            repository.save(
                KeyManagement(
                    type = request.type,
                    title = request.title,
                    methodFeature = request.methodFeature,
                    methodContent = request.methodContent,
                    methodDirection = request.methodDirection,
                    displayOrder = request.displayOrder,
                    fileId = request.fileId,
                ),
            )
        request.fileId?.let {
            fileService.finalizeUpload(it, "${KEY_MANAGEMENT}${savedKeyManagement.requiredId}/")
        }
        return savedKeyManagement.requiredId
    }

    @Transactional
    fun update(
        id: Long,
        request: KeyManagementUpdateRequest,
    ) {
        val keyManagement = getById(id)
        keyManagement.update(
            type = request.type,
            title = request.title,
            methodFeature = request.methodFeature,
            methodContent = request.methodContent,
            methodDirection = request.methodDirection,
        )

        if (keyManagement.fileId != request.fileId) {
            keyManagement.updateFileId(request.fileId)
            request.fileId?.let {
                fileService.finalizeUpload(it, "${KEY_MANAGEMENT}${keyManagement.requiredId}/")
            }
        }
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(getById(id).requiredId)
    }

    @Transactional
    fun select(id: Long) = getById(id).select()

    @Transactional
    fun deselect(id: Long) = getById(id).deselect()

    private fun getById(id: Long): KeyManagement =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_KEY_MANAGEMENT, id)

    private fun validateDisplayOrderUnique(
        type: KeyManagementType,
        displayOrder: Int,
    ) {
        if (repository.existsByTypeAndDisplayOrder(type, displayOrder)) {
            throw CustomException(YonginErrorCode.DUPLICATE_KEY_MANAGEMENT_DISPLAY_ORDER, type.description, displayOrder)
        }
    }
}
