package com.pluxity.yongin.notice.service

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.notice.dto.dummyNoticeRequest
import com.pluxity.yongin.notice.entity.Notice
import com.pluxity.yongin.notice.entity.dummyNotice
import com.pluxity.yongin.notice.repository.NoticeRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class NoticeServiceTest :
    BehaviorSpec({

        val repository: NoticeRepository = mockk()
        val service = NoticeService(repository)

        Given("공지사항 전체 조회") {

            When("공지사항 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyNotice(id = 1L, title = "공지사항 1"),
                        dummyNotice(id = 2L, title = "공지사항 2"),
                        dummyNotice(id = 3L, title = "공지사항 3"),
                    )
                val pageable = PageRequest.of(0, 9999)

                @Suppress("UNCHECKED_CAST")
                val page = PageImpl(entities, pageable, entities.size.toLong()) as Page<Notice?>

                every {
                    repository.findPage(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Notice>>>(),
                    )
                } returns page

                val result = service.findAll(PageSearchRequest(page = 1, size = 9999))

                Then("페이징된 결과가 반환된다") {
                    result.content.size shouldBe 3
                    result.totalElements shouldBe 3
                    result.pageNumber shouldBe 1
                }
            }
        }

        Given("공지사항 상세 조회") {

            When("존재하는 ID로 조회하면") {
                val entity = dummyNotice(id = 1L, title = "테스트 공지사항")

                every { repository.findByIdOrNull(1L) } returns entity

                val result = service.findById(1L)

                Then("해당 공지사항이 반환된다") {
                    result.id shouldBe 1L
                    result.title shouldBe "테스트 공지사항"
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

        Given("공지사항 등록") {

            When("새 공지사항을 등록하면") {
                val request = dummyNoticeRequest(title = "새 공지사항")
                val savedEntity = dummyNotice(id = 1L, title = "새 공지사항")

                every { repository.save(any()) } returns savedEntity

                val result = service.create(request)

                Then("저장된 ID가 반환된다") {
                    result shouldBe 1L
                    verify(exactly = 1) { repository.save(any()) }
                }
            }

            When("노출 상태와 게시기간을 포함하여 등록하면") {
                val today = LocalDate.now()
                val request =
                    dummyNoticeRequest(
                        title = "노출 공지",
                        isVisible = true,
                        isAlways = false,
                        startDate = today,
                        endDate = today.plusDays(30),
                    )
                val savedEntity =
                    dummyNotice(
                        id = 2L,
                        title = "노출 공지",
                        isVisible = true,
                        isAlways = false,
                        startDate = today,
                        endDate = today.plusDays(30),
                    )

                every { repository.save(any()) } returns savedEntity

                val result = service.create(request)

                Then("노출 상태와 게시기간이 포함되어 저장된다") {
                    result shouldBe 2L
                    verify(exactly = 1) {
                        repository.save(
                            match {
                                it.isVisible && !it.isAlways && it.startDate == today && it.endDate == today.plusDays(30)
                            },
                        )
                    }
                }
            }
        }

        Given("공지사항 수정") {

            When("존재하는 공지사항을 수정하면") {
                val existingEntity = dummyNotice(id = 1L, title = "기존 제목")
                val request = dummyNoticeRequest(title = "수정된 제목")

                every { repository.findByIdOrNull(1L) } returns existingEntity

                service.update(1L, request)

                Then("공지사항이 수정된다") {
                    existingEntity.title shouldBe "수정된 제목"
                }
            }

            When("노출 상태와 게시기간을 수정하면") {
                val today = LocalDate.now()
                val existingEntity = dummyNotice(id = 1L, title = "기존 제목", isVisible = false)
                val request =
                    dummyNoticeRequest(
                        title = "기존 제목",
                        isVisible = true,
                        isAlways = true,
                        startDate = today,
                        endDate = today.plusDays(7),
                    )

                every { repository.findByIdOrNull(1L) } returns existingEntity

                service.update(1L, request)

                Then("노출 상태와 게시기간이 수정된다") {
                    existingEntity.isVisible shouldBe true
                    existingEntity.isAlways shouldBe true
                    existingEntity.startDate shouldBe today
                    existingEntity.endDate shouldBe today.plusDays(7)
                }
            }

            When("존재하지 않는 공지사항을 수정하면") {
                val request = dummyNoticeRequest()

                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.update(999L, request)
                    }
                }
            }
        }

        Given("현재 노출 중인 공지사항 조회") {

            When("노출 중인 공지사항을 조회하면") {
                val today = LocalDate.now()
                val entities =
                    listOf(
                        dummyNotice(id = 1L, title = "상시 공지", isVisible = true, isAlways = true),
                        dummyNotice(
                            id = 2L,
                            title = "기간 공지",
                            isVisible = true,
                            isAlways = false,
                            startDate = today.minusDays(1),
                            endDate = today.plusDays(1),
                        ),
                    )

                every {
                    repository.findAll(
                        init = any<Jpql.() -> JpqlQueryable<SelectQuery<Notice>>>(),
                    )
                } returns entities

                val result = service.findActive()

                Then("노출 중인 공지사항 목록이 반환된다") {
                    result.size shouldBe 2
                    result[0].title shouldBe "상시 공지"
                    result[0].isVisible shouldBe true
                    result[0].isAlways shouldBe true
                    result[1].title shouldBe "기간 공지"
                    result[1].isVisible shouldBe true
                }
            }
        }

        Given("공지사항 게시기간 유효성 검증") {

            When("노출+기간 게시인데 시작일/종료일 둘 다 없으면") {
                val request =
                    dummyNoticeRequest(
                        isVisible = true,
                        isAlways = false,
                        startDate = null,
                        endDate = null,
                    )

                Then("INVALID_NOTICE_DATE_REQUIRED 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.create(request)
                        }
                    exception.code shouldBe YonginErrorCode.INVALID_NOTICE_DATE_REQUIRED
                }
            }

            When("노출+기간 게시인데 시작일이 종료일보다 이후이면") {
                val today = LocalDate.now()
                val request =
                    dummyNoticeRequest(
                        isVisible = true,
                        isAlways = false,
                        startDate = today.plusDays(10),
                        endDate = today,
                    )

                Then("INVALID_NOTICE_DATE_RANGE 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.create(request)
                        }
                    exception.code shouldBe YonginErrorCode.INVALID_NOTICE_DATE_RANGE
                }
            }

            When("노출+기간 게시인데 시작일만 있으면") {
                val today = LocalDate.now()
                val request =
                    dummyNoticeRequest(
                        isVisible = true,
                        isAlways = false,
                        startDate = today,
                        endDate = null,
                    )
                val savedEntity =
                    dummyNotice(id = 10L, title = "테스트 공지사항", isVisible = true, isAlways = false, startDate = today)

                every { repository.save(any()) } returns savedEntity

                val result = service.create(request)

                Then("정상적으로 저장된다") {
                    result shouldBe 10L
                }
            }

            When("노출+기간 게시인데 종료일만 있으면") {
                val today = LocalDate.now()
                val request =
                    dummyNoticeRequest(
                        isVisible = true,
                        isAlways = false,
                        startDate = null,
                        endDate = today.plusDays(30),
                    )
                val savedEntity =
                    dummyNotice(
                        id = 11L,
                        title = "테스트 공지사항",
                        isVisible = true,
                        isAlways = false,
                        endDate = today.plusDays(30),
                    )

                every { repository.save(any()) } returns savedEntity

                val result = service.create(request)

                Then("정상적으로 저장된다") {
                    result shouldBe 11L
                }
            }

            When("노출+상시 게시이면 날짜 없어도") {
                val request =
                    dummyNoticeRequest(
                        isVisible = true,
                        isAlways = true,
                        startDate = null,
                        endDate = null,
                    )
                val savedEntity = dummyNotice(id = 12L, title = "테스트 공지사항", isVisible = true, isAlways = true)

                every { repository.save(any()) } returns savedEntity

                val result = service.create(request)

                Then("정상적으로 저장된다") {
                    result shouldBe 12L
                }
            }

            When("미노출이면 날짜 없어도") {
                val request =
                    dummyNoticeRequest(
                        isVisible = false,
                        isAlways = false,
                        startDate = null,
                        endDate = null,
                    )
                val savedEntity = dummyNotice(id = 13L, title = "테스트 공지사항")

                every { repository.save(any()) } returns savedEntity

                val result = service.create(request)

                Then("정상적으로 저장된다") {
                    result shouldBe 13L
                }
            }

            When("수정 시 노출+기간 게시인데 시작일/종료일 둘 다 없으면") {
                val existingEntity = dummyNotice(id = 1L, title = "기존 제목")
                val request =
                    dummyNoticeRequest(
                        isVisible = true,
                        isAlways = false,
                        startDate = null,
                        endDate = null,
                    )

                every { repository.findByIdOrNull(1L) } returns existingEntity

                Then("INVALID_NOTICE_DATE_REQUIRED 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.update(1L, request)
                        }
                    exception.code shouldBe YonginErrorCode.INVALID_NOTICE_DATE_REQUIRED
                }
            }
        }

        Given("공지사항 삭제") {

            When("존재하는 공지사항을 삭제하면") {
                val entity = dummyNotice(id = 1L)

                every { repository.findByIdOrNull(1L) } returns entity
                every { repository.deleteById(1L) } just runs

                service.delete(1L)

                Then("삭제가 수행된다") {
                    verify(exactly = 1) { repository.deleteById(1L) }
                }
            }

            When("존재하지 않는 공지사항을 삭제하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    shouldThrow<CustomException> {
                        service.delete(999L)
                    }
                }
            }
        }
    })
