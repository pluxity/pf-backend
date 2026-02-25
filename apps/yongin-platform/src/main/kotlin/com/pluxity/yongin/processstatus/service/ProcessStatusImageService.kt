package com.pluxity.yongin.processstatus.service

import com.pluxity.common.file.service.FileService
import com.pluxity.yongin.processstatus.dto.ProcessStatusImageRequest
import com.pluxity.yongin.processstatus.dto.ProcessStatusImageResponse
import com.pluxity.yongin.processstatus.dto.toResponse
import com.pluxity.yongin.processstatus.entity.ProcessStatusImage
import com.pluxity.yongin.processstatus.repository.ProcessStatusImageRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProcessStatusImageService(
    private val repository: ProcessStatusImageRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val fileService: FileService,
) {
    companion object {
        private const val PROCESS_STATUS_IMAGE = "process-status-image/"
    }

    fun getImage(): ProcessStatusImageResponse {
        val image =
            repository.findByIdOrNull(ProcessStatusImage.SINGLETON_ID)
                ?: return ProcessStatusImageResponse()
        val fileResponse = fileService.getFileResponse(image.fileId)
        return image.toResponse(fileResponse)
    }

    @Transactional
    fun saveImage(request: ProcessStatusImageRequest) {
        repository
            .findByIdOrNull(ProcessStatusImage.SINGLETON_ID)
            ?.apply { update(request.fileId) }
            ?: repository.save(ProcessStatusImage(fileId = request.fileId))

        fileService.finalizeUpload(request.fileId, "$PROCESS_STATUS_IMAGE${ProcessStatusImage.SINGLETON_ID}/")
    }
}
