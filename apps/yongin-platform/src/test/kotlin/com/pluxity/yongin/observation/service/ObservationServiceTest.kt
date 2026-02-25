package com.pluxity.yongin.observation.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.entity.FileEntity
import com.pluxity.common.file.service.FileService
import com.pluxity.yongin.observation.dto.dummyObservationRequest
import com.pluxity.yongin.observation.entity.dummyObservation
import com.pluxity.yongin.observation.repository.ObservationRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class ObservationServiceTest :
    BehaviorSpec({

        val repository: ObservationRepository = mockk()
        val fileService: FileService = mockk()
        val service = ObservationService(repository, fileService)

        beforeSpec {
            every { fileService.getBaseUrl() } returns "/files"
        }

        Given("드론 관측 데이터 전체 조회") {

            When("전체 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyObservation(id = 1L, date = LocalDate.of(2026, 1, 27)),
                        dummyObservation(id = 2L, date = LocalDate.of(2026, 1, 28)),
                    )

                every { repository.findAll() } returns entities

                val result = service.findAll()

                Then("전체 목록이 반환된다") {
                    result.size shouldBe 2
                    result[0].id shouldBe 1L
                    result[1].id shouldBe 2L
                }
            }

            When("빈 목록을 조회하면") {
                every { repository.findAll() } returns emptyList()

                val result = service.findAll()

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("드론 관측 데이터 상세 조회") {

            When("존재하는 ID로 조회하면") {
                val entity = dummyObservation(id = 1L, directoryPath = "observation/1")

                every { repository.findByIdOrNull(1L) } returns entity

                val result = service.findById(1L)

                Then("해당 항목이 반환된다") {
                    result.id shouldBe 1L
                    result.filePath shouldBe "/files/observation/1/test_file"
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

        Given("드론 관측 데이터 등록") {

            When("새 항목을 등록하면") {
                val request = dummyObservationRequest(fileId = 100L)
                val savedEntity = dummyObservation(id = 1L, fileId = 100L)
                val mockFileEntity =
                    mockk<FileEntity> {
                        every { filePath } returns "observation/1/test_file.zip"
                    }

                every { repository.save(any()) } returns savedEntity
                every { fileService.finalizeUpload(100L, "observation/1/") } returns mockFileEntity

                val result = service.create(request)

                Then("저장된 ID가 반환된다") {
                    result shouldBe 1L
                    verify(exactly = 1) { repository.save(any()) }
                    verify(exactly = 1) { fileService.finalizeUpload(100L, "observation/1/") }
                }

                Then("directoryPath가 업데이트된다") {
                    savedEntity.directoryPath shouldBe "observation/1"
                }
            }
        }

        Given("드론 관측 데이터 수정") {

            When("존재하는 항목을 수정하면 (파일 변경 없음)") {
                val existingEntity = dummyObservation(id = 1L, fileId = 100L, description = "기존 설명")
                val request = dummyObservationRequest(fileId = 100L, description = "수정된 설명")

                every { repository.findByIdOrNull(1L) } returns existingEntity

                service.update(1L, request)

                Then("항목이 수정된다") {
                    existingEntity.description shouldBe "수정된 설명"
                }

                Then("finalizeUpload가 호출되지 않는다") {
                    verify(exactly = 0) { fileService.finalizeUpload(any(), any()) }
                }
            }

            When("존재하는 항목을 수정하면 (파일 변경 있음)") {
                val existingEntity = dummyObservation(id = 1L, fileId = 100L)
                val request = dummyObservationRequest(fileId = 200L)
                val mockFileEntity =
                    mockk<FileEntity> {
                        every { filePath } returns "observation/1/new_file.zip"
                    }

                every { repository.findByIdOrNull(1L) } returns existingEntity
                every { fileService.finalizeUpload(200L, "observation/1/") } returns mockFileEntity

                service.update(1L, request)

                Then("fileId가 변경된다") {
                    existingEntity.fileId shouldBe 200L
                }

                Then("finalizeUpload가 호출된다") {
                    verify(exactly = 1) { fileService.finalizeUpload(200L, "observation/1/") }
                }

                Then("directoryPath가 업데이트된다") {
                    existingEntity.directoryPath shouldBe "observation/1"
                }
            }

            When("존재하지 않는 항목을 수정하면") {
                val request = dummyObservationRequest()

                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.update(999L, request)
                    }
                }
            }
        }

        Given("드론 관측 데이터 삭제") {

            When("존재하는 항목을 삭제하면") {
                val entity = dummyObservation(id = 1L)

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
