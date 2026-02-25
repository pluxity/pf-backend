package com.pluxity.yongin.processstatus.service

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.common.file.entity.FileEntity
import com.pluxity.common.file.service.FileService
import com.pluxity.yongin.processstatus.dto.ProcessStatusImageRequest
import com.pluxity.yongin.processstatus.entity.ProcessStatusImage
import com.pluxity.yongin.processstatus.repository.ProcessStatusImageRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class ProcessStatusImageServiceTest :
    BehaviorSpec({

        val repository: ProcessStatusImageRepository = mockk()
        val fileService: FileService = mockk()
        val service = ProcessStatusImageService(repository, fileService)

        Given("공정관련 이미지 조회") {

            When("이미지가 존재하면") {
                val image = dummyProcessStatusImage(fileId = 100L)
                val fileResponse = FileResponse(id = 100L, originalFileName = "test.png", url = "/files/test.png")

                every { repository.findByIdOrNull(ProcessStatusImage.SINGLETON_ID) } returns image
                every { fileService.getFileResponse(100L) } returns fileResponse

                val result = service.getImage()

                Then("이미지 정보가 반환된다") {
                    result.fileId shouldBe 100L
                    result.file shouldNotBe null
                    result.file?.originalFileName shouldBe "test.png"
                }
            }

            When("이미지가 존재하지 않으면") {
                every { repository.findByIdOrNull(ProcessStatusImage.SINGLETON_ID) } returns null

                val result = service.getImage()

                Then("빈 응답이 반환된다") {
                    result.fileId shouldBe null
                }
            }
        }

        Given("공정관련 이미지 저장") {

            When("기존 이미지가 없으면") {
                val request = ProcessStatusImageRequest(fileId = 100L)
                val savedImage = dummyProcessStatusImage(fileId = 100L)
                val fileEntity = mockk<FileEntity>()

                every { repository.findByIdOrNull(ProcessStatusImage.SINGLETON_ID) } returns null
                every { repository.save(any()) } returns savedImage
                every { fileService.finalizeUpload(100L, any()) } returns fileEntity

                service.saveImage(request)

                Then("새 이미지가 저장되고 파일이 영구 저장된다") {
                    verify(exactly = 1) { repository.save(any()) }
                    verify(exactly = 1) { fileService.finalizeUpload(100L, "process-status-image/1/") }
                }
            }

            When("기존 이미지가 있으면") {
                val existingImage = dummyProcessStatusImage(fileId = 50L)
                val request = ProcessStatusImageRequest(fileId = 200L)
                val fileEntity = mockk<FileEntity>()

                every { repository.findByIdOrNull(ProcessStatusImage.SINGLETON_ID) } returns existingImage
                every { fileService.finalizeUpload(200L, any()) } returns fileEntity

                service.saveImage(request)

                Then("기존 이미지가 수정되고 파일이 영구 저장된다") {
                    existingImage.fileId shouldBe 200L
                    verify(exactly = 0) { repository.save(any()) }
                    verify(exactly = 1) { fileService.finalizeUpload(200L, "process-status-image/1/") }
                }
            }
        }
    })

private fun dummyProcessStatusImage(
    id: Long = ProcessStatusImage.SINGLETON_ID,
    fileId: Long = 1L,
): ProcessStatusImage =
    ProcessStatusImage(
        id = id,
        fileId = fileId,
    )
