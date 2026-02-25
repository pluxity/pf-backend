package com.pluxity.yongin.goal.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.goal.dto.dummyConstructionSectionRequest
import com.pluxity.yongin.goal.entity.dummyConstructionSection
import com.pluxity.yongin.goal.repository.ConstructionSectionRepository
import com.pluxity.yongin.goal.repository.GoalRepository
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

class ConstructionSectionServiceTest :
    BehaviorSpec({

        val repository: ConstructionSectionRepository = mockk()
        val goalRepository: GoalRepository = mockk()
        val service = ConstructionSectionService(repository, goalRepository)

        Given("시공구간 조회") {

            When("전체 목록을 조회하면") {
                val sections =
                    listOf(
                        dummyConstructionSection(id = 1L, name = "절토"),
                        dummyConstructionSection(id = 2L, name = "옹벽공"),
                        dummyConstructionSection(id = 3L, name = "도로공"),
                    )

                every { repository.findAll() } returns sections

                val result = service.findAll()

                Then("전체 시공구간 목록이 반환된다") {
                    result.size shouldBe 3
                    result[0].name shouldBe "절토"
                    result[1].name shouldBe "옹벽공"
                    result[2].name shouldBe "도로공"
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
                val section = dummyConstructionSection(id = 1L, name = "절토")

                every { repository.findByIdOrNull(1L) } returns section

                val result = service.getById(1L)

                Then("해당 시공구간이 반환된다") {
                    result.requiredId shouldBe 1L
                    result.name shouldBe "절토"
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

        Given("시공구간 등록") {

            When("새로운 시공구간을 등록하면") {
                val request = dummyConstructionSectionRequest(name = "절토")
                val savedSection = dummyConstructionSection(id = 1L, name = "절토")

                every { repository.save(any()) } returns savedSection

                val result = service.create(request)

                Then("등록된 시공구간 ID가 반환된다") {
                    result shouldBe 1L
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("중복된 이름으로 등록하면") {
                val request = dummyConstructionSectionRequest(name = "절토")

                every { repository.save(any()) } throws DataIntegrityViolationException("Duplicate entry")

                Then("DataIntegrityViolationException이 발생한다") {
                    shouldThrow<DataIntegrityViolationException> {
                        service.create(request)
                    }
                }
            }
        }

        Given("시공구간 삭제") {

            When("존재하는 시공구간을 삭제하면") {
                val section = dummyConstructionSection(id = 1L, name = "절토")

                every { repository.findByIdOrNull(1L) } returns section
                every { goalRepository.existsByConstructionSection(section) } returns false
                every { repository.deleteById(1L) } just runs

                service.delete(1L)

                Then("repository.deleteById가 호출된다") {
                    verify(exactly = 1) { repository.deleteById(1L) }
                }
            }

            When("존재하지 않는 시공구간을 삭제하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.delete(999L)
                    }
                }
            }

            When("목표관리에 등록된 시공구간을 삭제하면") {
                val section = dummyConstructionSection(id = 1L, name = "절토")

                every { repository.findByIdOrNull(1L) } returns section
                every { goalRepository.existsByConstructionSection(section) } returns true

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.delete(1L)
                    }
                }
            }
        }
    })
