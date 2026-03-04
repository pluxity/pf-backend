package com.pluxity.safers.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.cctv.client.CctvApiClient
import com.pluxity.safers.cctv.config.CctvErrorCode
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.cctv.entity.dummyCctv
import com.pluxity.safers.cctv.repository.CctvRepository
import com.pluxity.safers.site.entity.dummySite
import com.pluxity.safers.site.repository.SiteRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class CctvServiceTest :
    BehaviorSpec({

        val repository: CctvRepository = mockk(relaxed = true)
        val siteRepository: SiteRepository = mockk(relaxed = true)
        val fileService: FileService = mockk(relaxed = true)
        val apiClient: CctvApiClient = mockk()
        val service = CctvService(repository, fileService)
        val facade = CctvFacade(service, siteRepository, apiClient)

        val site = dummySite(id = 1L, baseUrl = "http://media-server:9997")

        mockkStatic("com.pluxity.common.core.utils.KotlinJdslExtensionsKt")

        Given("CCTV 동기화") {

            When("siteId를 지정하면 해당 site만 동기화한다") {
                val externalPaths =
                    listOf(
                        MediaServerPathItem(name = "cam1", confName = "conf1", ready = true),
                        MediaServerPathItem(name = "cam2", confName = "conf2", ready = true),
                    )

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { apiClient.fetchPaths("http://media-server:9997", 1L) } returns externalPaths
                every { repository.findBySiteId(1L) } returns emptyList()

                facade.sync(siteId = 1L)

                Then("새로운 CCTV가 저장된다") {
                    verify { repository.saveAll(match<List<Cctv>> { it.size == 2 }) }
                }
            }

            When("siteId 없이 호출하면 전체 site를 순회한다") {
                val externalPaths =
                    listOf(
                        MediaServerPathItem(name = "cam1", confName = "conf1", ready = true),
                    )

                every { siteRepository.findAll() } returns listOf(site)
                every { apiClient.fetchPaths("http://media-server:9997", 1L) } returns externalPaths
                every { repository.findBySiteId(1L) } returns emptyList()

                facade.sync()

                Then("전체 site에 대해 동기화가 수행된다") {
                    verify { repository.saveAll(match<List<Cctv>> { it.size == 1 }) }
                }
            }

            When("DB에 있지만 미디어서버에 없는 streamName이 있으면") {
                val existingCctv = dummyCctv(id = 1L, site = site, streamName = "cam_old")

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { apiClient.fetchPaths("http://media-server:9997", 1L) } returns
                    listOf(
                        MediaServerPathItem(name = "cam_new", confName = "conf1", ready = true),
                    )
                every { repository.findBySiteId(1L) } returns listOf(existingCctv)

                facade.sync(siteId = 1L)

                Then("해당 CCTV가 삭제된다") {
                    verify { repository.deleteAllInBatch(match<List<Cctv>> { it.size == 1 && it[0].streamName == "cam_old" }) }
                }
            }

            When("미디어서버와 DB가 동일하면") {
                clearMocks(repository, answers = false)
                val existingCctv = dummyCctv(id = 1L, site = site, streamName = "cam1")

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { apiClient.fetchPaths("http://media-server:9997", 1L) } returns
                    listOf(
                        MediaServerPathItem(name = "cam1", confName = "conf1", ready = true),
                    )
                every { repository.findBySiteId(1L) } returns listOf(existingCctv)

                facade.sync(siteId = 1L)

                Then("저장이나 삭제가 수행되지 않는다") {
                    verify(exactly = 0) { repository.saveAll(any<List<Cctv>>()) }
                    verify(exactly = 0) { repository.deleteAllInBatch(any<List<Cctv>>()) }
                }
            }

            When("존재하지 않는 siteId로 동기화하면") {
                every { siteRepository.findByIdOrNull(999L) } returns null

                Then("NOT_FOUND_SITE 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            facade.sync(siteId = 999L)
                        }
                    exception.code shouldBe CctvErrorCode.NOT_FOUND_SITE
                }
            }
        }

        Given("CCTV 목록 조회") {

            When("siteId로 조회하면 해당 site의 CCTV가 반환된다") {
                val entities =
                    listOf(
                        dummyCctv(id = 2L, site = site, streamName = "cam1", name = "A 카메라"),
                        dummyCctv(id = 1L, site = site, streamName = "cam2", name = "B 카메라"),
                    )

                every { repository.findAllNotNull<Cctv>(any()) } returns entities

                val result = service.findAll(siteId = 1L)

                Then("CCTV 목록이 반환된다") {
                    result.size shouldBe 2
                    result[0].name shouldBe "A 카메라"
                    result[1].name shouldBe "B 카메라"
                }
            }

            When("CCTV가 없으면") {
                every { repository.findAllNotNull<Cctv>(any()) } returns emptyList()

                val result = service.findAll(siteId = 1L)

                Then("빈 목록이 반환된다") {
                    result.size shouldBe 0
                }
            }
        }

        Given("CCTV 수정") {

            When("존재하는 CCTV를 수정하면") {
                val entity = dummyCctv(id = 1L, site = site, streamName = "cam1")
                val request = CctvUpdateRequest(name = "1번 카메라", lon = 127.0, lat = 37.0, alt = 50.0, nvrName = "NVR-01", channel = 1)

                every { repository.findByIdOrNull(1L) } returns entity

                service.update(1L, request)

                Then("name, lon, lat, alt, nvrName, channel이 수정된다") {
                    entity.name shouldBe "1번 카메라"
                    entity.lon shouldBe 127.0
                    entity.lat shouldBe 37.0
                    entity.alt shouldBe 50.0
                    entity.nvrName shouldBe "NVR-01"
                    entity.channel shouldBe 1
                }
            }

            When("존재하지 않는 CCTV를 수정하면") {
                every { repository.findByIdOrNull(999L) } returns null

                Then("NOT_FOUND_CCTV 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.update(
                                999L,
                                CctvUpdateRequest(name = "test", lon = null, lat = null, alt = null, nvrName = null, channel = null),
                            )
                        }
                    exception.code shouldBe CctvErrorCode.NOT_FOUND_CCTV
                }
            }
        }
    })
