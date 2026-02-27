package com.pluxity.common.file.service

import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.constant.FileStatus
import com.pluxity.common.file.properties.FileProperties
import com.pluxity.common.file.properties.LocalProperties
import com.pluxity.common.file.properties.S3Properties
import com.pluxity.common.file.repository.FileRepository
import com.pluxity.common.file.repository.ZipContentEntryRepository
import com.pluxity.common.file.strategy.storage.StorageStrategy
import com.pluxity.common.file.test.dummyFileEntity
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import org.springframework.mock.web.MockMultipartFile
import software.amazon.awssdk.services.s3.presigner.S3Presigner

class FileServiceKoTest :
    BehaviorSpec({
        val s3Presigner: S3Presigner = mockk()
        val s3Properties =
            S3Properties(
                bucket = "my-bucket",
                region = "ap-northeast-2",
                endpointUrl = "https://s3.ap-northeast-2.amazonaws.com",
                publicUrl = "https://my-cdn.com",
                accessKey = "accessKey",
                secretKey = "secretKey",
                preSignedUrlExpiration = 3600,
            )
        val storageStrategy: StorageStrategy = mockk()
        val repository: FileRepository = mockk()
        val zipContentEntryRepository: ZipContentEntryRepository = mockk()
        val fileProperties = FileProperties("local", LocalProperties("/tmp/files"))

        val fileService =
            FileService(
                s3Presigner,
                s3Properties,
                storageStrategy,
                repository,
                zipContentEntryRepository,
                fileProperties,
            )

        Given("파일 업로드 시작을 진행할 때") {
            When("유효한 파일로 업로드 요청") {
                val mockFile =
                    MockMultipartFile("file", "test.png", "image/png", "test-content".toByteArray())
                val savedFile = dummyFileEntity()
                val tempPath = "temp/test-uuid.png"

                every { storageStrategy.save(any()) } returns tempPath
                every { repository.save(any()) } returns savedFile

                Then("파일 엔티티를 생성하고 ID를 반환한다") {
                    val fileId = fileService.initiateUpload(mockFile)
                    fileId shouldNotBe null
                    verify(exactly = 1) { storageStrategy.save(any()) }
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("originalFilename이 없는 파일로 업로드 요청") {
                val mockFile =
                    MockMultipartFile("file", null as String?, "image/png", "test-content".toByteArray())

                Then("FAILED_TO_UPLOAD_FILE 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        fileService.initiateUpload(mockFile)
                    }.code shouldBe ErrorCode.FAILED_TO_UPLOAD_FILE
                }
            }
        }

        Given("파일 영구 저장을 진행할 때") {
            When("임시 상태의 파일을 영구 저장 요청") {
                val tempFile = dummyFileEntity(filePath = "temp/some-temp-file.png")
                val permanentPath = "permanent/test-file.png"

                every { repository.findByIdOrNull(any()) } returns tempFile
                every { storageStrategy.persist(any()) } returns permanentPath

                Then("상태와 경로가 업데이트된다") {
                    val result = fileService.finalizeUpload(tempFile.requiredId, permanentPath)
                    result.fileStatus shouldBe FileStatus.COMPLETE
                    result.filePath shouldBe permanentPath
                }
            }

            When("존재하지 않는 파일 ID로 영구 저장 요청") {
                every { repository.findByIdOrNull(any()) } returns null

                Then("예외가 발생한다") {
                    shouldThrowExactly<CustomException> {
                        fileService.finalizeUpload(9999L, "some/path")
                    }.code shouldBe ErrorCode.INVALID_FILE_STATUS
                }
            }

            When("임시 상태가 아닌 파일을 영구 저장 요청") {
                val completeFile = dummyFileEntity()
                completeFile.makeComplete("already/persisted.png")

                every { repository.findByIdOrNull(any()) } returns completeFile

                Then("INVALID_FILE_STATUS 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        fileService.finalizeUpload(completeFile.requiredId, "new/path")
                    }.code shouldBe ErrorCode.INVALID_FILE_STATUS
                }
            }
        }

        Given("파일 조회를 진행할 때") {
            When("존재하는 파일 ID로 조회 요청") {
                val file = dummyFileEntity()
                every { repository.findByIdOrNull(any()) } returns file

                Then("FileEntity를 반환한다") {
                    val result = fileService.getFile(file.requiredId)
                    result.id shouldBe file.id
                }
            }

            When("존재하지 않는 파일 ID로 조회 요청") {
                every { repository.findByIdOrNull(any()) } returns null

                Then("NOT_FOUND_FILE 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        fileService.getFile(9999L)
                    }.code shouldBe ErrorCode.NOT_FOUND_FILE
                }
            }
        }

        Given("파일 목록 조회를 진행할 때") {
            When("ID 목록으로 조회 요청") {
                val file1 = dummyFileEntity(id = 1L, filePath = "temp/file1.png")
                val file2 = dummyFileEntity(id = 2L, filePath = "temp/file2.png")

                every { repository.findByIdIn(any()) } returns listOf(file1, file2)
                every { zipContentEntryRepository.findByFileIdIn(any()) } returns emptyList()

                Then("FileResponse 목록을 반환한다") {
                    val result = fileService.getFiles(listOf(1L, 2L))
                    result.size shouldBe 2
                }
            }

            When("빈 ID 목록으로 조회 요청") {
                Then("빈 리스트를 반환한다") {
                    val result = fileService.getFiles(emptyList())
                    result.size shouldBe 0
                }
            }
        }

        Given("FileResponse 생성을 진행할 때") {
            When("로컬 저장 전략일 때") {
                val file = dummyFileEntity(filePath = "uploads/test.png")

                every { repository.findByIdOrNull(any()) } returns file
                every { zipContentEntryRepository.findByFileId(any()) } returns emptyList()

                Then("올바른 URL 형식을 반환한다") {
                    val response = fileService.getFileResponse(file.requiredId)
                    response?.url shouldBe "/files/uploads/test.png"
                    response?.id shouldBe file.id
                }
            }

            When("null ID로 조회 요청") {
                Then("null을 반환한다") {
                    val response = fileService.getFileResponse(null)
                    response shouldBe null
                }
            }
        }

        Given("getBaseUrl을 호출할 때") {
            When("로컬 전략이면") {
                Then("/files를 반환한다") {
                    fileService.getBaseUrl() shouldBe "/files"
                }
            }
        }
    })
