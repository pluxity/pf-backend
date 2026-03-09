package com.pluxity.yongin.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.client.CctvApiClient
import com.pluxity.yongin.cctv.dto.CctvUpdateRequest
import com.pluxity.yongin.cctv.dto.MediaServerPathItem
import com.pluxity.yongin.cctv.entity.Cctv
import com.pluxity.yongin.cctv.entity.dummyCctv
import com.pluxity.yongin.cctv.entity.dummyCctvBookmark
import com.pluxity.yongin.cctv.repository.CctvBookmarkRepository
import com.pluxity.yongin.cctv.repository.CctvRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class CctvServiceTest :
    BehaviorSpec({

        val repository: CctvRepository = mockk(relaxed = true)
        val bookmarkRepository: CctvBookmarkRepository = mockk(relaxed = true)
        val apiClient: CctvApiClient = mockk()
        val service = CctvService(repository, bookmarkRepository, apiClient)

        Given("CCTV 동기화") {

            When("미디어서버에 새로운 streamName이 있으면") {
                val externalPaths =
                    listOf(
                        MediaServerPathItem(name = "cam1", confName = "conf1", ready = true),
                        MediaServerPathItem(name = "cam2", confName = "conf2", ready = true),
                    )

                every { apiClient.fetchPaths() } returns externalPaths
                every { repository.findAll() } returns emptyList()

                service.sync()

                Then("새로운 CCTV가 저장된다") {
                    verify { repository.saveAll(match<List<Cctv>> { it.size == 2 }) }
                }
            }

            When("DB에 있지만 미디어서버에 없는 streamName이 있으면") {
                val existingCctv = dummyCctv(id = 1L, streamName = "cam_old")

                every { apiClient.fetchPaths() } returns
                    listOf(
                        MediaServerPathItem(name = "cam_new", confName = "conf1", ready = true),
                    )
                every { repository.findAll() } returns listOf(existingCctv)

                service.sync()

                Then("해당 CCTV가 삭제된다") {
                    verify { repository.deleteAllInBatch(match<List<Cctv>> { it.size == 1 && it[0].streamName == "cam_old" }) }
                }
            }

            When("미디어서버와 DB가 동일하면") {
                clearMocks(repository, answers = false)
                val existingCctv = dummyCctv(id = 1L, streamName = "cam1")

                every { apiClient.fetchPaths() } returns
                    listOf(
                        MediaServerPathItem(name = "cam1", confName = "conf1", ready = true),
                    )
                every { repository.findAll() } returns listOf(existingCctv)

                service.sync()

                Then("저장이나 삭제가 수행되지 않는다") {
                    verify(exactly = 0) { repository.saveAll(any<List<Cctv>>()) }
                    verify(exactly = 0) { repository.deleteAllInBatch(any<List<Cctv>>()) }
                }
            }
        }

        Given("CCTV 목록 조회") {

            When("즐겨찾기가 있으면 즐겨찾기 순서대로 먼저 나온다") {
                val entities =
                    listOf(
                        dummyCctv(id = 1L, streamName = "cam1", name = "A 카메라"),
                        dummyCctv(id = 2L, streamName = "cam2", name = "B 카메라"),
                        dummyCctv(id = 3L, streamName = "cam3", name = "C 카메라"),
                    )
                val bookmarks =
                    listOf(
                        dummyCctvBookmark(id = 1L, streamName = "cam3", displayOrder = 1),
                        dummyCctvBookmark(id = 2L, streamName = "cam1", displayOrder = 2),
                    )

                every { repository.findAll() } returns entities
                every { bookmarkRepository.findAllByOrderByDisplayOrderAsc() } returns bookmarks

                val result = service.findAll()

                Then("즐겨찾기(displayOrder순) → 나머지(name순)으로 정렬된다") {
                    result.size shouldBe 3
                    result[0].streamName shouldBe "cam3"
                    result[1].streamName shouldBe "cam1"
                    result[2].streamName shouldBe "cam2"
                }
            }

            When("즐겨찾기가 없으면 이름순으로 반환된다") {
                val entities =
                    listOf(
                        dummyCctv(id = 1L, streamName = "cam2", name = "B 카메라"),
                        dummyCctv(id = 2L, streamName = "cam1", name = "A 카메라"),
                    )

                every { repository.findAll() } returns entities
                every { bookmarkRepository.findAllByOrderByDisplayOrderAsc() } returns emptyList()

                val result = service.findAll()

                Then("이름순으로 반환된다") {
                    result.size shouldBe 2
                    result[0].name shouldBe "A 카메라"
                    result[1].name shouldBe "B 카메라"
                }
            }

            When("CCTV가 없으면") {
                every { repository.findAll() } returns emptyList()
                every { bookmarkRepository.findAllByOrderByDisplayOrderAsc() } returns emptyList()

                val result = service.findAll()

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("CCTV 수정") {

            When("존재하는 CCTV를 수정하면") {
                val entity = dummyCctv(id = 1L, streamName = "cam1")
                val request = CctvUpdateRequest(name = "1번 카메라", lon = 127.0, lat = 37.0, alt = 50.0)

                every { repository.findByIdOrNull(1L) } returns entity

                service.update(1L, request)

                Then("name, lon, lat, alt가 수정된다") {
                    entity.name shouldBe "1번 카메라"
                    entity.lon shouldBe 127.0
                    entity.lat shouldBe 37.0
                    entity.alt shouldBe 50.0
                }
            }

            When("존재하지 않는 CCTV를 수정하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("NOT_FOUND_CCTV 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.update(999L, CctvUpdateRequest(name = "test", lon = null, lat = null, alt = null))
                        }
                    exception.code shouldBe YonginErrorCode.NOT_FOUND_CCTV
                }
            }
        }
    })
