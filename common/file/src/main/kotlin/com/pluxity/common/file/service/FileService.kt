package com.pluxity.common.file.service

import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.constant.FileStatus
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.common.file.dto.ZipEntryInfo
import com.pluxity.common.file.dto.toResponse
import com.pluxity.common.file.entity.FileEntity
import com.pluxity.common.file.entity.ZipContentEntry
import com.pluxity.common.file.properties.FileProperties
import com.pluxity.common.file.properties.S3Properties
import com.pluxity.common.file.repository.FileRepository
import com.pluxity.common.file.repository.ZipContentEntryRepository
import com.pluxity.common.file.strategy.storage.FilePersistenceContext
import com.pluxity.common.file.strategy.storage.FileProcessingContext
import com.pluxity.common.file.strategy.storage.StorageStrategy
import com.pluxity.common.file.utils.FileUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.zip.ZipFile

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class FileService(
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties,
    private val storageStrategy: StorageStrategy,
    private val repository: FileRepository,
    private val zipContentEntryRepository: ZipContentEntryRepository,
    private val fileProperties: FileProperties,
) {
    fun generatePreSignedUrl(s3Key: String): String {
        val getObjectRequest =
            GetObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(s3Key)
                .build()

        val presignRequest =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.preSignedUrlExpiration.toLong()))
                .getObjectRequest(getObjectRequest)
                .build()

        val preSignedUrl = s3Presigner.presignGetObject(presignRequest).url()
        return preSignedUrl.toString()
    }

    @Transactional
    fun initiateUpload(file: MultipartFile): Long {
        val originalFileName =
            file.originalFilename
                ?.takeIf { it.isNotBlank() }
                ?: throw CustomException(ErrorCode.FAILED_TO_UPLOAD_FILE, "originalFilename is missing")

        val tempPath = FileUtils.createTempFile(originalFileName)
        file.transferTo(tempPath)

        val zipRootContents =
            originalFileName
                .takeIf { it.endsWith(".zip", ignoreCase = true) }
                ?.let { getZipRootContents(tempPath.toFile()) }
                ?: emptyList()

        try {
            val contentType = FileUtils.getContentType(file)
            val context =
                FileProcessingContext(
                    contentType = contentType,
                    tempPath = tempPath,
                    originalFileName = originalFileName,
                )

            val filePath = storageStrategy.save(context)

            val fileEntity =
                FileEntity(
                    filePath = filePath,
                    originalFileName = originalFileName,
                    contentType = contentType,
                )

            val savedFile = repository.save(fileEntity)
            saveZipContents(savedFile, zipRootContents)

            return savedFile.requiredId
        } catch (e: Exception) {
            log.error { "File Upload Exception : ${e.message}" }
            throw CustomException(ErrorCode.FAILED_TO_UPLOAD_FILE, e.message)
        } finally {
            Files.deleteIfExists(tempPath)
        }
    }

    @Transactional
    fun initiateUpload(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): Long {
        val tempPath = FileUtils.createTempFile(fileName)
        Files.write(tempPath, bytes)

        try {
            val context =
                FileProcessingContext(
                    contentType = contentType,
                    tempPath = tempPath,
                    originalFileName = fileName,
                )

            val filePath = storageStrategy.save(context)

            val fileEntity =
                FileEntity(
                    filePath = filePath,
                    originalFileName = fileName,
                    contentType = contentType,
                )

            return repository.save(fileEntity).requiredId
        } catch (e: Exception) {
            log.error { "File Upload Exception : ${e.message}" }
            throw CustomException(ErrorCode.FAILED_TO_UPLOAD_FILE, e.message)
        } finally {
            Files.deleteIfExists(tempPath)
        }
    }

    private fun saveZipContents(
        file: FileEntity,
        contents: List<ZipEntryInfo>,
    ) {
        if (contents.isEmpty()) return
        val entries = contents.map { ZipContentEntry(file, it.name, it.isDirectory) }
        zipContentEntryRepository.saveAll(entries)
    }

    private fun getZipRootContents(zipFile: File): List<ZipEntryInfo> =
        ZipFile(zipFile).use { zip ->
            val rootItems = mutableMapOf<String, Boolean>()

            zip.entries().asSequence().forEach { entry ->
                val name = entry.name
                when (val slashIndex = name.indexOf('/')) {
                    -1 -> rootItems.putIfAbsent(name, false)
                    else -> rootItems[name.substring(0, slashIndex)] = true
                }
            }

            rootItems.map { (name, isDirectory) -> ZipEntryInfo(name, isDirectory) }
        }

    @Transactional
    fun finalizeUpload(
        fileId: Long,
        newPath: String,
    ): FileEntity {
        try {
            val file =
                repository
                    .findByIdOrNull(fileId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_FILE, fileId)

            require(file.fileStatus == FileStatus.TEMP) {
                throw CustomException(ErrorCode.INVALID_FILE_STATUS, "임시 파일이 아닌 경우에는 영구 저장할 수 없습니다")
            }

            val context =
                FilePersistenceContext(
                    filePath = file.filePath,
                    newPath = newPath,
                    contentType = file.contentType,
                    originalFileName = file.originalFileName,
                )

            val persistPath = storageStrategy.persist(context)

            file.makeComplete(persistPath)
            return file
        } catch (e: Exception) {
            log.error { "File Persist Exception : $e.message" }
            throw CustomException(ErrorCode.INVALID_FILE_STATUS, e.message)
        }
    }

    fun getFile(fileId: Long): FileEntity =
        repository
            .findByIdOrNull(fileId)
            ?: throw CustomException(ErrorCode.NOT_FOUND_FILE, fileId)

    fun getFiles(fileIds: List<Long>): List<FileResponse> {
        if (fileIds.isEmpty()) return emptyList()

        val files = repository.findByIdIn(fileIds)
        val zipEntriesMap =
            zipContentEntryRepository
                .findByFileIdIn(fileIds)
                .groupBy { it.file.requiredId }

        return files.mapNotNull { file ->
            buildFileResponse(file, zipEntriesMap[file.requiredId] ?: emptyList())
        }
    }

    fun getFileResponse(fileId: Long?): FileResponse? =
        fileId?.let { id ->
            val file = getFile(id)
            val zipContentEntries = zipContentEntryRepository.findByFileId(id)
            buildFileResponse(file, zipContentEntries)
        }

    private fun buildFileResponse(
        fileEntity: FileEntity?,
        zipContentEntries: List<ZipContentEntry> = emptyList(),
    ): FileResponse? =
        fileEntity?.let { file ->
            file.toResponse("${getBaseUrl()}/${file.filePath}", zipContentEntries)
        }

    fun getBaseUrl(): String =
        if ("local" == fileProperties.storageStrategy) {
            "/files"
        } else {
            "${s3Properties.publicUrl}/${s3Properties.bucket}"
        }
}
