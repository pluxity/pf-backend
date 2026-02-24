package com.pluxity.yonginplatform.observation.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.service.FileService
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import com.pluxity.yonginplatform.observation.dto.ObservationRequest
import com.pluxity.yonginplatform.observation.dto.ObservationResponse
import com.pluxity.yonginplatform.observation.dto.toResponse
import com.pluxity.yonginplatform.observation.entity.Observation
import com.pluxity.yonginplatform.observation.repository.ObservationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ObservationService(
    private val repository: ObservationRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val fileService: FileService,
) {
    companion object {
        private const val OBSERVATION_PATH: String = "observation/"
    }

    fun findAll(): List<ObservationResponse> = repository.findAll().map { it.toResponse(fileService.getBaseUrl()) }

    fun findById(id: Long): ObservationResponse = getById(id).toResponse(fileService.getBaseUrl())

    @Transactional
    fun create(request: ObservationRequest): Long {
        val savedObservation =
            repository.save(
                Observation(
                    date = request.date,
                    description = request.description,
                    fileId = request.fileId,
                    rootFileName = request.rootFileName,
                ),
            )
        finalizeAndUpdatePath(savedObservation, request.fileId)
        return savedObservation.requiredId
    }

    @Transactional
    fun update(
        id: Long,
        request: ObservationRequest,
    ) {
        val observation = getById(id)
        val previousFileId = observation.fileId

        observation.update(
            date = request.date,
            description = request.description,
            fileId = request.fileId,
            rootFileName = request.rootFileName,
        )

        if (previousFileId != request.fileId) {
            finalizeAndUpdatePath(observation, request.fileId)
        }
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(getById(id).requiredId)
    }

    private fun getById(id: Long): Observation =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_OBSERVATION, id)

    private fun finalizeAndUpdatePath(
        observation: Observation,
        fileId: Long,
    ) {
        val fileEntity = fileService.finalizeUpload(fileId, "${OBSERVATION_PATH}${observation.requiredId}/")
        val directoryPath = fileEntity.filePath.substringBeforeLast("/")
        observation.updateDirectoryPath(directoryPath)
    }
}
