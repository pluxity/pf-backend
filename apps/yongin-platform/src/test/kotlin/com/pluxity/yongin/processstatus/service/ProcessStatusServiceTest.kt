package com.pluxity.yongin.processstatus.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.processstatus.dto.dummyPageSearchRequest
import com.pluxity.yongin.processstatus.dto.dummyProcessStatusBulkRequest
import com.pluxity.yongin.processstatus.dto.dummyProcessStatusRequest
import com.pluxity.yongin.processstatus.entity.ProcessStatus
import com.pluxity.yongin.processstatus.entity.dummyProcessStatus
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

class ProcessStatusServiceTest :
    BehaviorSpec({

        val repository: ProcessStatusRepository = mockk()
        val workTypeRepository: WorkTypeRepository = mockk()
        val service = ProcessStatusService(repository, workTypeRepository)

        val earthwork = dummyWorkType(id = 1L, name = "토공")
        val road = dummyWorkType(id = 2L, name = "도로공")
        val nonOpenCut = dummyWorkType(id = 3L, name = "비개착")
        val bridgeRetainingWall = dummyWorkType(id = 4L, name = "교량/옹벽")

        Given("공정현황 조회") {

            When("전체 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyProcessStatus(id = 1L, workType = earthwork, workDate = LocalDate.of(2026, 1, 15)),
                        dummyProcessStatus(id = 2L, workType = road, workDate = LocalDate.of(2026, 1, 14)),
                        dummyProcessStatus(id = 3L, workType = nonOpenCut, workDate = LocalDate.of(2026, 1, 13)),
                    )

                val page =
                    PageImpl(
                        entities,
                        PageRequest.of(0, 10),
                        entities.size.toLong(),
                    )
                every {
                    repository.findAllOrderByWorkDateDesc(any())
                } returns page

                val result = service.findAll(dummyPageSearchRequest())

                Then("페이징된 결과가 반환된다") {
                    result.content.size shouldBe 3
                    result.pageNumber shouldBe 1
                    result.pageSize shouldBe 10
                    result.totalElements shouldBe 3
                }
            }

            When("빈 목록을 조회하면") {
                val pageable = PageRequest.of(0, 10)

                val page = PageImpl(emptyList<ProcessStatus>(), pageable, 0)
                every {
                    repository.findAllOrderByWorkDateDesc(any())
                } returns page

                val result = service.findAll(dummyPageSearchRequest())

                Then("빈 결과가 반환된다") {
                    result.content.size shouldBe 0
                    result.totalElements shouldBe 0
                }
            }

            When("페이지 번호를 지정하여 조회하면") {
                val entities = (11L..15L).map { dummyProcessStatus(id = it, workType = bridgeRetainingWall) }

                val page = PageImpl(entities, PageRequest.of(1, 10), 15)
                every {
                    repository.findAllOrderByWorkDateDesc(any())
                } returns page

                val result = service.findAll(dummyPageSearchRequest(page = 2))

                Then("해당 페이지의 결과가 반환된다") {
                    result.content.size shouldBe 5
                    result.pageNumber shouldBe 2
                    result.totalElements shouldBe 15
                    result.first shouldBe false
                }
            }

            When("최근 데이터를 조회하면") {
                val latestDate = LocalDate.of(2026, 1, 15)
                val entities =
                    listOf(
                        dummyProcessStatus(id = 1L, workType = earthwork, workDate = latestDate),
                        dummyProcessStatus(id = 2L, workType = road, workDate = latestDate),
                        dummyProcessStatus(id = 3L, workType = nonOpenCut, workDate = latestDate),
                    )

                every { repository.findAllByLatestWorkDate() } returns entities

                val result = service.findLatest()

                Then("최근 날짜의 데이터 목록이 반환된다") {
                    result.size shouldBe 3
                    result.all { it.workDate == latestDate } shouldBe true
                }
            }

            When("데이터가 없는 상태에서 최근 데이터를 조회하면") {
                every { repository.findAllByLatestWorkDate() } returns emptyList()

                val result = service.findLatest()

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("공정현황 일괄 저장/수정/삭제") {

            When("id가 없는 데이터를 저장하면") {
                val request =
                    dummyProcessStatusBulkRequest(
                        upserts = listOf(dummyProcessStatusRequest(workTypeId = 1L)),
                    )

                every { workTypeRepository.findAllById(listOf(1L)) } returns listOf(earthwork)
                every { repository.findAllById(emptyList()) } returns emptyList()
                every { repository.save(any()) } returns dummyProcessStatus()

                service.saveOrUpdateAll(request)

                Then("repository.save가 호출된다") {
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("id가 있는 데이터를 수정하면") {
                val existingEntity =
                    dummyProcessStatus(
                        id = 1L,
                        workType = earthwork,
                        workDate = LocalDate.of(2026, 1, 10),
                        plannedRate = 80,
                        actualRate = 75,
                    )

                val request =
                    dummyProcessStatusBulkRequest(
                        upserts =
                            listOf(
                                dummyProcessStatusRequest(
                                    id = 1L,
                                    workTypeId = 2L,
                                ),
                            ),
                    )

                every { workTypeRepository.findAllById(listOf(2L)) } returns listOf(road)
                every { repository.findAllById(listOf(1L)) } returns listOf(existingEntity)

                service.saveOrUpdateAll(request)

                Then("엔티티가 업데이트된다") {
                    existingEntity.workType shouldBe road
                    existingEntity.plannedRate shouldBe 100
                    existingEntity.actualRate shouldBe 100
                }
            }

            When("존재하지 않는 id로 수정하면") {
                val request =
                    dummyProcessStatusBulkRequest(
                        upserts =
                            listOf(
                                dummyProcessStatusRequest(id = 999L, workTypeId = 1L),
                            ),
                    )

                every { workTypeRepository.findAllById(listOf(1L)) } returns listOf(earthwork)
                every { repository.findAllById(listOf(999L)) } returns emptyList()

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.saveOrUpdateAll(request)
                    }
                }
            }

            When("삭제할 id 목록이 있으면") {
                val request =
                    dummyProcessStatusBulkRequest(
                        deletedIds = listOf(1L, 2L, 3L),
                    )

                every { repository.deleteAllById(listOf(1L, 2L, 3L)) } just runs

                service.saveOrUpdateAll(request)

                Then("repository.deleteAllById가 호출된다") {
                    verify(exactly = 1) { repository.deleteAllById(listOf(1L, 2L, 3L)) }
                }
            }

            When("isActive가 true인 데이터를 저장하면") {
                val request =
                    dummyProcessStatusBulkRequest(
                        upserts = listOf(dummyProcessStatusRequest(workTypeId = 1L, isActive = true)),
                    )

                val savedEntity = dummyProcessStatus(isActive = true)

                every { workTypeRepository.findAllById(listOf(1L)) } returns listOf(earthwork)
                every { repository.findAllById(emptyList()) } returns emptyList()
                every { repository.save(any()) } returns savedEntity

                service.saveOrUpdateAll(request)

                Then("isActive가 true로 저장된다") {
                    verify(exactly = 1) { repository.save(match { it.isActive }) }
                }
            }

            When("isActive를 false에서 true로 수정하면") {
                val existingEntity =
                    dummyProcessStatus(
                        id = 1L,
                        workType = earthwork,
                        isActive = false,
                    )

                val request =
                    dummyProcessStatusBulkRequest(
                        upserts =
                            listOf(
                                dummyProcessStatusRequest(
                                    id = 1L,
                                    workTypeId = 1L,
                                    isActive = true,
                                ),
                            ),
                    )

                every { workTypeRepository.findAllById(listOf(1L)) } returns listOf(earthwork)
                every { repository.findAllById(listOf(1L)) } returns listOf(existingEntity)

                service.saveOrUpdateAll(request)

                Then("isActive가 true로 변경된다") {
                    existingEntity.isActive shouldBe true
                }
            }

            When("저장, 수정, 삭제가 동시에 요청되면") {
                val existingEntity =
                    dummyProcessStatus(
                        id = 2L,
                        workType = road,
                        plannedRate = 50,
                        actualRate = 45,
                    )

                val request =
                    dummyProcessStatusBulkRequest(
                        upserts =
                            listOf(
                                dummyProcessStatusRequest(workTypeId = 1L),
                                dummyProcessStatusRequest(id = 2L, workTypeId = 3L),
                            ),
                        deletedIds = listOf(5L, 6L),
                    )

                every { repository.deleteAllById(listOf(5L, 6L)) } just runs
                every { workTypeRepository.findAllById(listOf(1L, 3L)) } returns listOf(earthwork, nonOpenCut)
                every { repository.findAllById(listOf(2L)) } returns listOf(existingEntity)
                every { repository.save(any()) } returns existingEntity

                service.saveOrUpdateAll(request)

                Then("삭제, 저장, 수정이 모두 수행된다") {
                    verify(exactly = 1) { repository.deleteAllById(listOf(5L, 6L)) }
                    verify(exactly = 1) { repository.save(any()) }
                    existingEntity.workType shouldBe nonOpenCut
                }
            }
        }
    })
