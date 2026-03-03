package com.pluxity.yongin.systemsetting.service

import com.pluxity.common.file.service.FileService
import com.pluxity.yongin.systemsetting.dto.SystemSettingRequest
import com.pluxity.yongin.systemsetting.dto.SystemSettingResponse
import com.pluxity.yongin.systemsetting.dto.toResponse
import com.pluxity.yongin.systemsetting.entity.SystemSetting
import com.pluxity.yongin.systemsetting.repository.SystemSettingRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SystemSettingService(
    private val systemSettingRepository: SystemSettingRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val fileService: FileService,
) {
    companion object {
        private const val BIM_THUMBNAIL_PATH = "system-settings/bim-thumbnail/"
        private const val AERIAL_VIEW_PATH = "system-settings/aerial-view/"
    }

    fun find(): SystemSettingResponse {
        val setting =
            systemSettingRepository.findByIdOrNull(SystemSetting.SINGLETON_ID)
                ?: return SystemSettingResponse()
        val bimThumbnailFile = fileService.getFileResponse(setting.bimThumbnailFileId)
        val aerialViewFile = fileService.getFileResponse(setting.aerialViewFileId)
        return setting.toResponse(bimThumbnailFile, aerialViewFile)
    }

    @Transactional
    fun update(request: SystemSettingRequest) {
        val existing = systemSettingRepository.findByIdOrNull(SystemSetting.SINGLETON_ID)
        val oldBimThumbnailFileId = existing?.bimThumbnailFileId
        val oldAerialViewFileId = existing?.aerialViewFileId

        val setting =
            existing
                ?.apply {
                    update(
                        rollingIntervalSeconds = request.rollingIntervalSeconds,
                        bimThumbnailFileId = request.bimThumbnailFileId,
                        aerialViewFileId = request.aerialViewFileId,
                    )
                }
                ?: systemSettingRepository.save(
                    SystemSetting(
                        rollingIntervalSeconds = request.rollingIntervalSeconds,
                        bimThumbnailFileId = request.bimThumbnailFileId,
                        aerialViewFileId = request.aerialViewFileId,
                    ),
                )

        request.bimThumbnailFileId
            ?.takeIf { it != oldBimThumbnailFileId }
            ?.let { fileService.finalizeUpload(it, "$BIM_THUMBNAIL_PATH${setting.id}/") }
        request.aerialViewFileId
            ?.takeIf { it != oldAerialViewFileId }
            ?.let { fileService.finalizeUpload(it, "$AERIAL_VIEW_PATH${setting.id}/") }
    }
}
