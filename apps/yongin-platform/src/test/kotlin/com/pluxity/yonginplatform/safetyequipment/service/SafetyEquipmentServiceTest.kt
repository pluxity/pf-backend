package com.pluxity.yonginplatform.safetyequipment.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import com.pluxity.yonginplatform.safetyequipment.dto.dummySafetyEquipmentRequest
import com.pluxity.yonginplatform.safetyequipment.entity.dummySafetyEquipment
import com.pluxity.yonginplatform.safetyequipment.repository.SafetyEquipmentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class SafetyEquipmentServiceTest :
    BehaviorSpec({

        val safetyEquipmentRepository: SafetyEquipmentRepository = mockk(relaxed = true)

        val service = SafetyEquipmentService(safetyEquipmentRepository)

        Given("안전장비 생성") {

            When("안전장비를 생성하면") {
                val request = dummySafetyEquipmentRequest()
                val saved = dummySafetyEquipment(id = 1L)

                every { safetyEquipmentRepository.save(any()) } returns saved

                val result = service.create(request)

                Then("안전장비 ID가 반환된다") {
                    result shouldBe 1L
                }
            }
        }

        Given("안전장비 단건 조회") {

            When("존재하는 안전장비를 조회하면") {
                val safetyEquipment = dummySafetyEquipment(id = 1L, name = "안전모", quantity = 100)

                every { safetyEquipmentRepository.findByIdOrNull(1L) } returns safetyEquipment

                val result = service.findById(1L)

                Then("안전장비 정보가 반환된다") {
                    result.id shouldBe 1L
                    result.name shouldBe "안전모"
                    result.quantity shouldBe 100
                }
            }

            When("존재하지 않는 안전장비를 조회하면") {
                every { safetyEquipmentRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.findById(999L)
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_SAFETY_EQUIPMENT
                }
            }
        }

        Given("안전장비 전체 조회") {

            When("안전장비 목록을 조회하면") {
                val equipments =
                    listOf(
                        dummySafetyEquipment(id = 1L, name = "안전모", quantity = 100),
                        dummySafetyEquipment(id = 2L, name = "안전벨트", quantity = 50),
                        dummySafetyEquipment(id = 3L, name = "안전화", quantity = 200),
                    )

                every { safetyEquipmentRepository.findAll() } returns equipments

                val result = service.findAll()

                Then("전체 목록이 반환된다") {
                    result.size shouldBe 3
                }
            }

            When("안전장비가 없으면") {
                every { safetyEquipmentRepository.findAll() } returns mutableListOf()

                val result = service.findAll()

                Then("빈 결과가 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("안전장비 수정") {

            When("존재하는 안전장비를 수정하면") {
                val safetyEquipment = dummySafetyEquipment(id = 1L, name = "안전모", quantity = 100)
                val request = dummySafetyEquipmentRequest(name = "안전모 (수정)", quantity = 200)

                every { safetyEquipmentRepository.findByIdOrNull(1L) } returns safetyEquipment

                service.update(1L, request)

                Then("안전장비 정보가 수정된다") {
                    safetyEquipment.name shouldBe "안전모 (수정)"
                    safetyEquipment.quantity shouldBe 200
                }
            }

            When("존재하지 않는 안전장비를 수정하면") {
                val request = dummySafetyEquipmentRequest()

                every { safetyEquipmentRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.update(999L, request)
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_SAFETY_EQUIPMENT
                }
            }
        }

        Given("안전장비 삭제") {

            When("존재하는 안전장비를 삭제하면") {
                val safetyEquipment = dummySafetyEquipment(id = 1L)

                every { safetyEquipmentRepository.findByIdOrNull(1L) } returns safetyEquipment

                service.delete(1L)

                Then("안전장비가 삭제된다") {
                    verify { safetyEquipmentRepository.delete(safetyEquipment) }
                }
            }

            When("존재하지 않는 안전장비를 삭제하면") {
                every { safetyEquipmentRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.delete(999L)
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_SAFETY_EQUIPMENT
                }
            }
        }
    })
