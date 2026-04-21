package com.pluxity.safers.event.service

import com.pluxity.common.file.service.FileService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

private val log = KotlinLogging.logger {}

@Service
class EventFileDownloadService(
    private val fileService: FileService,
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 30_000
        const val MAX_DOWNLOAD_SIZE = 50 * 1024 * 1024
        private const val BUFFER_SIZE = 8 * 1024
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }

    fun downloadAndInitiateUpload(fileUrl: String): Long? =
        try {
            val uri = URI.create(fileUrl)
            when {
                uri.scheme == null || uri.authority == null -> {
                    log.warn { "유효하지 않은 파일 URL (scheme 또는 authority 누락): $fileUrl" }
                    null
                }
                uri.scheme.lowercase() !in ALLOWED_SCHEMES -> {
                    log.warn { "허용되지 않는 스킴: ${uri.scheme}, url: $fileUrl" }
                    null
                }
                else -> download(uri, fileUrl)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to download and upload file: $fileUrl" }
            null
        }

    private fun download(
        uri: URI,
        fileUrl: String,
    ): Long? {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                log.warn { "비정상 응답 코드: $responseCode, url: $fileUrl" }
                return null
            }
            val fileBytes =
                connection.inputStream.use { readBounded(it) } ?: run {
                    log.warn { "파일 크기 한도 초과 (> ${MAX_DOWNLOAD_SIZE}B): $fileUrl" }
                    return null
                }
            val fileName = uri.path.substringAfterLast('/')
            val contentType = determineContentType(fileName)
            fileService.initiateUpload(fileBytes, fileName, contentType)
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            total += n
            if (total > MAX_DOWNLOAD_SIZE) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
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
