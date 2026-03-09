package com.pluxity.yongin.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.config.CctvProperties
import com.pluxity.yongin.cctv.dto.CctvBookmarkOrderRequest
import com.pluxity.yongin.cctv.dto.CctvBookmarkRequest
import com.pluxity.yongin.cctv.entity.dummyCctvBookmark
import com.pluxity.yongin.cctv.repository.CctvBookmarkRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class CctvBookmarkServiceTest :
    BehaviorSpec({

        val repository: CctvBookmarkRepository = mockk(relaxed = true)
        val cctvProperties = CctvProperties(maxBookmarkCount = 4)
        val service = CctvBookmarkService(repository, cctvProperties)

        Given("즐겨찾기 목록 조회") {

            When("즐겨찾기가 있으면") {
                val entities =
                    listOf(
                        dummyCctvBookmark(id = 1L, streamName = "cam1", displayOrder = 1),
                        dummyCctvBookmark(id = 2L, streamName = "cam2", displayOrder = 2),
                    )

                every { repository.findAllByOrderByDisplayOrderAsc() } returns entities

                val result = service.findAll()

                Then("순서대로 반환된다") {
                    result.size shouldBe 2
                    result[0].displayOrder shouldBe 1
                    result[1].displayOrder shouldBe 2
                }
            }

            When("즐겨찾기가 없으면") {
                every { repository.findAllByOrderByDisplayOrderAsc() } returns emptyList()

                val result = service.findAll()

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("즐겨찾기 등록") {

            When("정상적으로 등록하면") {
                val request = CctvBookmarkRequest(streamName = "cam1")
                val saved = dummyCctvBookmark(id = 1L, streamName = "cam1", displayOrder = 1)

                every { repository.existsByStreamName("cam1") } returns false
                every { repository.count() } returns 0
                every { repository.save(any()) } returns saved

                val result = service.create(request)

                Then("ID가 반환된다") {
                    result shouldBe 1L
                }
            }

            When("이미 즐겨찾기된 경로를 등록하면") {
                val request = CctvBookmarkRequest(streamName = "cam1")

                every { repository.existsByStreamName("cam1") } returns true

                Then("ALREADY_BOOKMARK 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.create(request)
                        }
                    exception.code shouldBe YonginErrorCode.ALREADY_BOOKMARK
                }
            }

            When("즐겨찾기가 4개인 상태에서 등록하면") {
                val request = CctvBookmarkRequest(streamName = "cam5")

                every { repository.existsByStreamName("cam5") } returns false
                every { repository.count() } returns 4

                Then("EXCEED_BOOKMARK_LIMIT 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.create(request)
                        }
                    exception.code shouldBe YonginErrorCode.EXCEED_BOOKMARK_LIMIT
                }
            }
        }

        Given("즐겨찾기 삭제") {

            When("존재하는 즐겨찾기를 삭제하면") {
                val entity = dummyCctvBookmark(id = 1L, streamName = "cam1")

                every { repository.findByIdOrNull(1L) } returns entity

                service.delete(1L)

                Then("삭제가 수행된다") {
                    verify { repository.delete(entity) }
                }
            }

            When("존재하지 않는 즐겨찾기를 삭제하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("NOT_FOUND_CCTV_BOOKMARK 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.delete(999L)
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_CCTV_BOOKMARK
                }
            }
        }

        Given("즐겨찾기 순서 변경") {

            When("순서를 변경하면") {
                val bm1 = dummyCctvBookmark(id = 1L, streamName = "cam1", displayOrder = 1)
                val bm2 = dummyCctvBookmark(id = 2L, streamName = "cam2", displayOrder = 2)
                val request = CctvBookmarkOrderRequest(ids = listOf(2L, 1L))

                every { repository.count() } returns 2
                every { repository.findAllById(listOf(2L, 1L)) } returns listOf(bm1, bm2)

                service.updateOrder(request)

                Then("순서가 변경된다") {
                    bm2.displayOrder shouldBe 1
                    bm1.displayOrder shouldBe 2
                }
            }

            When("일부 ID만 포함되면") {
                val request = CctvBookmarkOrderRequest(ids = listOf(1L))

                every { repository.count() } returns 3

                Then("INVALID_BOOKMARK_ORDER_COUNT 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.updateOrder(request)
                        }
                    exception.code shouldBe YonginErrorCode.INVALID_BOOKMARK_ORDER_COUNT
                }
            }

            When("존재하지 않는 ID가 포함되면") {
                val bm1 = dummyCctvBookmark(id = 1L, streamName = "cam1", displayOrder = 1)
                val request = CctvBookmarkOrderRequest(ids = listOf(1L, 999L))

                every { repository.count() } returns 2
                every { repository.findAllById(listOf(1L, 999L)) } returns listOf(bm1)

                Then("NOT_FOUND_CCTV_BOOKMARK 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.updateOrder(request)
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_CCTV_BOOKMARK
                }
            }
        }
    })
