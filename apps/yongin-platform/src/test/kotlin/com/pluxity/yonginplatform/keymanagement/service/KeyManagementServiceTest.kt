package com.pluxity.yonginplatform.keymanagement.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.common.file.service.FileService
import com.pluxity.yonginplatform.keymanagement.dto.KeyManagementUpdateRequest
import com.pluxity.yonginplatform.keymanagement.dto.dummyKeyManagementRequest
import com.pluxity.yonginplatform.keymanagement.entity.KeyManagementType
import com.pluxity.yonginplatform.keymanagement.entity.dummyKeyManagement
import com.pluxity.yonginplatform.keymanagement.repository.KeyManagementRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class KeyManagementServiceTest :
    BehaviorSpec({

        val repository: KeyManagementRepository = mockk()
        val fileService: FileService = mockk()
        val service = KeyManagementService(repository, fileService)

        Given("주요관리사항 전체 조회") {

            When("전체 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyKeyManagement(id = 1L, type = KeyManagementType.QUALITY, displayOrder = 1),
                        dummyKeyManagement(id = 2L, type = KeyManagementType.QUALITY, displayOrder = 2),
                        dummyKeyManagement(id = 3L, type = KeyManagementType.SAFETY, displayOrder = 1),
                    )

                every { repository.findAll() } returns entities
                every { fileService.getFiles(any<List<Long>>()) } returns emptyList()

                val result = service.findAll()

                Then("타입별로 그룹화된 결과가 반환된다") {
                    result.size shouldBe KeyManagementType.entries.size
                    result.find { it.type == KeyManagementType.QUALITY }?.items?.size shouldBe 2
                    result.find { it.type == KeyManagementType.SAFETY }?.items?.size shouldBe 1
                    result.find { it.type == KeyManagementType.METHOD }?.items?.size shouldBe 0
                }
            }

            When("파일이 있는 항목을 조회하면") {
                val entities =
                    listOf(
                        dummyKeyManagement(id = 1L, type = KeyManagementType.QUALITY, fileId = 100L),
                    )

                val fileResponse = FileResponse(id = 100L, url = "example.com/file.pdf")

                every { repository.findAll() } returns entities
                every { fileService.getFiles(listOf(100L)) } returns listOf(fileResponse)

                val result = service.findAll()

                Then("파일 정보가 포함된다") {
                    val qualityItems = result.find { it.type == KeyManagementType.QUALITY }?.items
                    qualityItems?.first()?.file?.id shouldBe 100L
                }
            }
        }

        Given("대시보드용 선택된 항목 조회") {

            When("선택된 항목만 조회하면") {
                val entities =
                    listOf(
                        dummyKeyManagement(id = 1L, type = KeyManagementType.QUALITY, selected = true, displayOrder = 1),
                        dummyKeyManagement(id = 2L, type = KeyManagementType.SAFETY, selected = true, displayOrder = 1),
                    )

                every { repository.findBySelectedTrue() } returns entities
                every { fileService.getFiles(any<List<Long>>()) } returns emptyList()

                val result = service.findSelected()

                Then("선택된 항목만 반환된다") {
                    result.size shouldBe 2
                    result.all { it.selected } shouldBe true
                }
            }
        }

        Given("주요관리사항 상세 조회") {

            When("존재하는 ID로 조회하면") {
                val entity = dummyKeyManagement(id = 1L, type = KeyManagementType.QUALITY)

                every { repository.findByIdOrNull(1L) } returns entity
                every { fileService.getFiles(any<List<Long>>()) } returns emptyList()

                val result = service.findById(1L)

                Then("해당 항목이 반환된다") {
                    result.id shouldBe 1L
                    result.type shouldBe KeyManagementType.QUALITY
                }
            }

            When("존재하지 않는 ID로 조회하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.findById(999L)
                    }
                }
            }
        }

        Given("주요관리사항 등록") {

            When("새 항목을 등록하면") {
                val request = dummyKeyManagementRequest(type = KeyManagementType.QUALITY, title = "품질관리")
                val savedEntity = dummyKeyManagement(id = 1L, type = KeyManagementType.QUALITY, title = "품질관리")

                every { repository.existsByTypeAndDisplayOrder(KeyManagementType.QUALITY, 1) } returns false
                every { repository.save(any()) } returns savedEntity
                every { fileService.getFiles(any<List<Long>>()) } returns emptyList()

                val result = service.create(request)

                Then("저장된 ID가 반환된다") {
                    result shouldBe 1L
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("같은 타입에 중복된 순서로 등록하면") {
                val request = dummyKeyManagementRequest(type = KeyManagementType.QUALITY, displayOrder = 1)

                every { repository.existsByTypeAndDisplayOrder(KeyManagementType.QUALITY, 1) } returns true

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.create(request)
                    }
                }
            }
        }

        Given("주요관리사항 수정") {

            When("존재하는 항목을 수정하면") {
                val existingEntity = dummyKeyManagement(id = 1L, type = KeyManagementType.QUALITY, title = "기존 제목")
                val request = dummyKeyManagementRequest(type = KeyManagementType.SAFETY, title = "수정된 제목")

                every { repository.findByIdOrNull(1L) } returns existingEntity
                every { fileService.getFiles(any<List<Long>>()) } returns emptyList()

                service.update(
                    1L,
                    KeyManagementUpdateRequest(
                        type = request.type,
                        title = request.title,
                        methodFeature = request.methodFeature,
                        methodContent = request.methodContent,
                        methodDirection = request.methodDirection,
                        fileId = request.fileId,
                    ),
                )

                Then("항목이 수정된다") {
                    existingEntity.type shouldBe KeyManagementType.SAFETY
                    existingEntity.title shouldBe "수정된 제목"
                }
            }

            When("존재하지 않는 항목을 수정하면") {
                val request = dummyKeyManagementRequest()

                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.update(
                            999L,
                            KeyManagementUpdateRequest(
                                type = request.type,
                                title = request.title,
                                methodFeature = request.methodFeature,
                                methodContent = request.methodContent,
                                methodDirection = request.methodDirection,
                                fileId = request.fileId,
                            ),
                        )
                    }
                }
            }
        }

        Given("주요관리사항 삭제") {

            When("존재하는 항목을 삭제하면") {
                val entity = dummyKeyManagement(id = 1L)
                every { repository.findByIdOrNull(1L) } returns entity
                every { repository.deleteById(1L) } just runs

                service.delete(1L)

                Then("삭제가 수행된다") {
                    verify(exactly = 1) { repository.deleteById(1L) }
                }
            }

            When("존재하지 않는 항목을 삭제하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.delete(999L)
                    }
                }
            }
        }
    })
