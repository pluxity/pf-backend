package com.pluxity.safers.event.service

import com.pluxity.common.file.service.FileService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URI

private val log = KotlinLogging.logger {}

@Service
class EventFileDownloadService(
    private val fileService: FileService,
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    fun downloadAndInitiateUpload(fileUrl: String): Long? =
        try {
            val uri = URI.create(fileUrl)
            if (uri.scheme == null || uri.authority == null) {
                log.warn { "유효하지 않은 파일 URL (scheme 또는 authority 누락): $fileUrl" }
                return null
            }
            val fileName = uri.path.substringAfterLast('/')
            val fileBytes =
                uri
                    .toURL()
                    .openConnection()
                    .apply {
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                    }.getInputStream()
                    .use { it.readAllBytes() }

            val contentType = determineContentType(fileName)
            fileService.initiateUpload(fileBytes, fileName, contentType)
        } catch (e: Exception) {
            log.error(e) { "Failed to download and upload file: $fileUrl" }
            null
        }

    private fun determineContentType(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
}
