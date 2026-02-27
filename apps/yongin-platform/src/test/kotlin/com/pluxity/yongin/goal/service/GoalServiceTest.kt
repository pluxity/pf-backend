package com.pluxity.yongin.goal.service

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.goal.dto.dummyGoalBulkRequest
import com.pluxity.yongin.goal.dto.dummyGoalRequest
import com.pluxity.yongin.goal.dto.dummyPageSearchRequest
import com.pluxity.yongin.goal.entity.Goal
import com.pluxity.yongin.goal.entity.dummyConstructionSection
import com.pluxity.yongin.goal.entity.dummyGoal
import com.pluxity.yongin.goal.repository.ConstructionSectionRepository
import com.pluxity.yongin.goal.repository.GoalRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class GoalServiceTest :
    BehaviorSpec({

        mockkStatic("com.pluxity.common.core.utils.KotlinJdslExtensionsKt")

        val repository: GoalRepository = mockk()
        val constructionSectionRepository: ConstructionSectionRepository = mockk()
        val service = GoalService(repository, constructionSectionRepository)

        val section1 = dummyConstructionSection(id = 1L, name = "절토")
        val section2 = dummyConstructionSection(id = 2L, name = "도로공")
        val section3 = dummyConstructionSection(id = 3L, name = "교량공")

        Given("목표관리 조회") {

            When("전체 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyGoal(id = 1L, constructionSection = section1),
                        dummyGoal(id = 2L, constructionSection = section2),
                        dummyGoal(id = 3L, constructionSection = section3),
                    )

                val page =
                    PageImpl(
                        entities,
                        PageRequest.of(0, 10),
                        entities.size.toLong(),
                    )

                every {
                    repository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Goal>>>(),
                    )
                } returns page

                val result =
                    service.findAll(dummyPageSearchRequest())

                Then("페이징된 결과가 반환된다") {
                    result.content.size shouldBe 3
                    result.pageNumber shouldBe 1
                    result.pageSize shouldBe 10
                    result.totalElements shouldBe 3
                }
            }

            When("빈 목록을 조회하면") {
                val page =
                    PageImpl(
                        emptyList<Goal>(),
                        PageRequest.of(0, 10),
                        0,
                    )

                every {
                    repository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Goal>>>(),
                    )
                } returns page

                val result =
                    service.findAll(dummyPageSearchRequest())

                Then("빈 결과가 반환된다") {
                    result.content.size shouldBe 0
                    result.totalElements shouldBe 0
                }
            }

            When("페이지 번호를 지정하여 조회하면") {
                val entities =
                    (11L..15L).map {
                        dummyGoal(id = it, constructionSection = section1)
                    }

                val page =
                    PageImpl(
                        entities,
                        PageRequest.of(1, 10),
                        15,
                    )

                every {
                    repository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Goal>>>(),
                    )
                } returns page

                val result =
                    service.findAll(dummyPageSearchRequest(page = 2))

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
                        dummyGoal(id = 1L, constructionSection = section1, inputDate = latestDate),
                        dummyGoal(id = 2L, constructionSection = section2, inputDate = latestDate),
                        dummyGoal(id = 3L, constructionSection = section3, inputDate = latestDate),
                    )

                every { repository.findAllByLatestInputDate() } returns entities

                val result = service.findLatest()

                Then("최근 날짜의 데이터 목록이 반환된다") {
                    result.size shouldBe 3
                    result.all { it.inputDate == latestDate } shouldBe true
                }
            }

            When("데이터가 없는 상태에서 최근 데이터를 조회하면") {
                every { repository.findAllByLatestInputDate() } returns emptyList()

                val result = service.findLatest()

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("목표관리 일괄 저장/수정/삭제") {

            When("id가 없는 데이터를 저장하면") {
                val request =
                    dummyGoalBulkRequest(
                        upserts = listOf(dummyGoalRequest(constructionSectionId = 1L)),
                    )

                every { constructionSectionRepository.findAllById(listOf(1L)) } returns listOf(section1)
                every { repository.findAllById(emptyList()) } returns emptyList()
                every { repository.save(any()) } returns dummyGoal(constructionSection = section1)

                service.saveOrUpdateAll(request)

                Then("repository.save가 호출된다") {
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("id가 있는 데이터를 수정하면") {
                val existingEntity =
                    dummyGoal(
                        id = 1L,
                        constructionSection = section1,
                        progressRate = 50.0f,
                    )

                val request =
                    dummyGoalBulkRequest(
                        upserts =
                            listOf(
                                dummyGoalRequest(
                                    id = 1L,
                                    constructionSectionId = 2L,
                                    progressRate = 100.0f,
                                ),
                            ),
                    )

                every { constructionSectionRepository.findAllById(listOf(2L)) } returns listOf(section2)
                every { repository.findAllById(listOf(1L)) } returns listOf(existingEntity)

                service.saveOrUpdateAll(request)

                Then("엔티티가 업데이트된다") {
                    existingEntity.constructionSection shouldBe section2
                    existingEntity.progressRate shouldBe 100.0f
                }
            }

            When("존재하지 않는 id로 수정하면") {
                val request =
                    dummyGoalBulkRequest(
                        upserts =
                            listOf(
                                dummyGoalRequest(id = 999L, constructionSectionId = 1L),
                            ),
                    )

                every { constructionSectionRepository.findAllById(listOf(1L)) } returns listOf(section1)
                every { repository.findAllById(listOf(999L)) } returns emptyList()

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.saveOrUpdateAll(request)
                    }
                }
            }

            When("삭제할 id 목록이 있으면") {
                val request =
                    dummyGoalBulkRequest(
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
                    dummyGoalBulkRequest(
                        upserts = listOf(dummyGoalRequest(constructionSectionId = 1L, isActive = true)),
                    )

                val savedEntity = dummyGoal(constructionSection = section1, isActive = true)

                every { constructionSectionRepository.findAllById(listOf(1L)) } returns listOf(section1)
                every { repository.findAllById(emptyList()) } returns emptyList()
                every { repository.save(any()) } returns savedEntity

                service.saveOrUpdateAll(request)

                Then("isActive가 true로 저장된다") {
                    verify(exactly = 1) { repository.save(match { it.isActive }) }
                }
            }

            When("isActive를 false에서 true로 수정하면") {
                val existingEntity =
                    dummyGoal(
                        id = 1L,
                        constructionSection = section1,
                        isActive = false,
                    )

                val request =
                    dummyGoalBulkRequest(
                        upserts =
                            listOf(
                                dummyGoalRequest(
                                    id = 1L,
                                    constructionSectionId = 1L,
                                    isActive = true,
                                ),
                            ),
                    )

                every { constructionSectionRepository.findAllById(listOf(1L)) } returns listOf(section1)
                every { repository.findAllById(listOf(1L)) } returns listOf(existingEntity)

                service.saveOrUpdateAll(request)

                Then("isActive가 true로 변경된다") {
                    existingEntity.isActive shouldBe true
                }
            }

            When("저장, 수정, 삭제가 동시에 요청되면") {
                val existingEntity =
                    dummyGoal(
                        id = 2L,
                        constructionSection = section2,
                        progressRate = 30.0f,
                    )

                val request =
                    dummyGoalBulkRequest(
                        upserts =
                            listOf(
                                dummyGoalRequest(constructionSectionId = 3L),
                                dummyGoalRequest(
                                    id = 2L,
                                    constructionSectionId = 3L,
                                    progressRate = 100.0f,
                                ),
                            ),
                        deletedIds = listOf(5L, 6L),
                    )

                every { repository.deleteAllById(listOf(5L, 6L)) } just runs
                every { constructionSectionRepository.findAllById(listOf(3L)) } returns listOf(section3)
                every { repository.findAllById(listOf(2L)) } returns listOf(existingEntity)
                every { repository.save(any()) } returns existingEntity

                service.saveOrUpdateAll(request)

                Then("삭제, 저장, 수정이 모두 수행된다") {
                    verify(exactly = 1) { repository.deleteAllById(listOf(5L, 6L)) }
                    verify(exactly = 1) { repository.save(any()) }
                    existingEntity.constructionSection shouldBe section3
                }
            }
        }
    })
