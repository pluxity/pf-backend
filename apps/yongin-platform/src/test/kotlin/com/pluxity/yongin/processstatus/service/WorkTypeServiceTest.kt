package com.pluxity.yongin.processstatus.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.processstatus.dto.dummyWorkTypeRequest
import com.pluxity.yongin.processstatus.entity.dummyWorkType
import com.pluxity.yongin.processstatus.repository.ProcessStatusRepository
import com.pluxity.yongin.processstatus.repository.WorkTypeRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull

class WorkTypeServiceTest :
    BehaviorSpec({

        val repository: WorkTypeRepository = mockk()
        val processStatusRepository: ProcessStatusRepository = mockk()
        val service = WorkTypeService(repository, processStatusRepository)

        Given("공정명 조회") {

            When("전체 목록을 조회하면") {
                val workTypes =
                    listOf(
                        dummyWorkType(id = 1L, name = "토공"),
                        dummyWorkType(id = 2L, name = "도로공"),
                        dummyWorkType(id = 3L, name = "비개착"),
                    )

                every { repository.findAll() } returns workTypes

                val result = service.findAll()

                Then("전체 공정명 목록이 반환된다") {
                    result.size shouldBe 3
                    result[0].name shouldBe "토공"
                    result[1].name shouldBe "도로공"
                    result[2].name shouldBe "비개착"
                }
            }

            When("빈 목록을 조회하면") {
                every { repository.findAll() } returns emptyList()

                val result = service.findAll()

                Then("빈 결과가 반환된다") {
                    result.size shouldBe 0
                }
            }

            When("ID로 조회하면") {
                val workType = dummyWorkType(id = 1L, name = "토공")

                every { repository.findByIdOrNull(1L) } returns workType

                val result = service.getById(1L)

                Then("해당 공정명이 반환된다") {
                    result.requiredId shouldBe 1L
                    result.name shouldBe "토공"
                }
            }

            When("존재하지 않는 ID로 조회하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.getById(999L)
                    }
                }
            }
        }

        Given("공정명 등록") {

            When("새로운 공정명을 등록하면") {
                val request = dummyWorkTypeRequest(name = "토공")
                val savedWorkType = dummyWorkType(id = 1L, name = "토공")

                every { repository.save(any()) } returns savedWorkType

                val result = service.create(request)

                Then("등록된 공정명 ID가 반환된다") {
                    result shouldBe 1L
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("중복된 이름으로 등록하면") {
                val request = dummyWorkTypeRequest(name = "토공")

                every { repository.save(any()) } throws DataIntegrityViolationException("Duplicate entry")

                Then("DataIntegrityViolationException이 발생한다") {
                    shouldThrow<DataIntegrityViolationException> {
                        service.create(request)
                    }
                }
            }
        }

        Given("공정명 삭제") {

            When("존재하는 공정명을 삭제하면") {
                val workType = dummyWorkType(id = 1L, name = "토공")

                every { repository.findByIdOrNull(1L) } returns workType
                every { processStatusRepository.existsByWorkType(workType) } returns false
                every { repository.deleteById(1L) } just runs

                service.delete(1L)

                Then("repository.deleteById가 호출된다") {
                    verify(exactly = 1) { repository.deleteById(1L) }
                }
            }

            When("존재하지 않는 공정명을 삭제하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.delete(999L)
                    }
                }
            }

            When("공정현황에 등록된 공정명을 삭제하면") {
                val workType = dummyWorkType(id = 1L, name = "토공")

                every { repository.findByIdOrNull(1L) } returns workType
                every { processStatusRepository.existsByWorkType(workType) } returns true

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.delete(1L)
                    }
                }
            }
        }
    })
