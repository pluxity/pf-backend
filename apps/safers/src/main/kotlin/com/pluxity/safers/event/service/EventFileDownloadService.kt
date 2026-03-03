package com.pluxity.safers.event.service

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.global.properties.EventProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

@Service
class EventFileDownloadService(
    private val fileService: FileService,
    eventProperties: EventProperties,
    webClientFactory: WebClientFactory,
) {
    private val webClient: WebClient = webClientFactory.createClient(eventProperties.baseUrl)

    fun downloadAndInitiateUpload(
        remotePath: String,
        fileName: String,
    ): Long? =
        try {
            val sanitizedFileName = Paths.get(fileName).fileName.toString()
            val fileBytes =
                webClient
                    .get()
                    .uri("$remotePath$sanitizedFileName")
                    .retrieve()
                    .bodyToMono<ByteArray>()
                    .block()
                    ?: error("파일 다운로드에 실패했습니다: $remotePath$sanitizedFileName")

            val contentType = determineContentType(sanitizedFileName)
            fileService.initiateUpload(fileBytes, sanitizedFileName, contentType)
        } catch (e: Exception) {
            log.error(e) { "Failed to download and upload file: $remotePath$fileName" }
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
