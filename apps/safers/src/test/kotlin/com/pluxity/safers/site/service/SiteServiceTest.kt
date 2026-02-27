package com.pluxity.safers.site.service

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.common.file.service.FileService
import com.pluxity.common.test.dto.dummyFileResponse
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.site.dto.dummySiteRequest
import com.pluxity.safers.site.entity.Region
import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.site.entity.dummySite
import com.pluxity.safers.site.repository.SiteRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull

class SiteServiceTest :
    BehaviorSpec({

        mockkStatic("com.pluxity.common.core.utils.KotlinJdslExtensionsKt")

        val siteRepository: SiteRepository = mockk(relaxed = true)
        val fileService: FileService = mockk(relaxed = true)
        val service = SiteService(siteRepository, fileService)

        Given("현장 생성") {

            When("썸네일 이미지가 포함된 현장을 생성하면") {
                val request = dummySiteRequest(thumbnailImageId = 10L)
                val savedSite = dummySite(id = 1L, thumbnailImageId = 10L)

                every { siteRepository.save(any()) } returns savedSite

                val result = service.create(request)

                Then("현장 ID가 반환된다") {
                    result shouldBe 1L
                }

                Then("파일 업로드가 확정된다") {
                    verify { fileService.finalizeUpload(10L, "sites/1/") }
                }
            }

            When("썸네일 이미지 없이 현장을 생성하면") {
                val request = dummySiteRequest(thumbnailImageId = null)
                val savedSite = dummySite(id = 2L)

                every { siteRepository.save(any()) } returns savedSite

                val result = service.create(request)

                Then("현장 ID가 반환된다") {
                    result shouldBe 2L
                }

                Then("파일 업로드가 수행되지 않는다") {
                    verify(exactly = 0) { fileService.finalizeUpload(any(), eq("sites/2/")) }
                }
            }
        }

        Given("현장 단건 조회") {

            When("존재하는 현장을 조회하면") {
                val site = dummySite(id = 1L, thumbnailImageId = 10L)
                val thumbnailFileResponse = dummyFileResponse(id = 10L)

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { fileService.getFileResponse(10L) } returns thumbnailFileResponse

                val result = service.findById(1L)

                Then("현장 정보가 반환된다") {
                    result.id shouldBe 1L
                    result.name shouldBe "서울역 현장"
                    result.region shouldBe Region.SEOUL
                    result.location shouldContain "POLYGON"
                    result.thumbnailImage shouldBe thumbnailFileResponse
                }
            }

            When("존재하지 않는 현장을 조회하면") {
                every { siteRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.findById(999L)
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_SITE
                }
            }
        }

        Given("현장 전체 조회") {

            When("현장 목록을 조회하면") {
                val sites =
                    listOf(
                        dummySite(id = 1L, name = "현장 1", thumbnailImageId = 10L),
                        dummySite(id = 2L, name = "현장 2", thumbnailImageId = 11L),
                        dummySite(id = 3L, name = "현장 3", thumbnailImageId = 12L),
                    )

                val page = PageImpl(sites)

                every {
                    siteRepository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Site>>>(),
                    )
                } returns page

                every { fileService.getFiles(any()) } returns
                    listOf(
                        dummyFileResponse(id = 10L),
                        dummyFileResponse(id = 11L),
                        dummyFileResponse(id = 12L),
                    )

                val result = service.findAll(PageSearchRequest(page = 1, size = 10))

                Then("페이징된 결과가 반환된다") {
                    result.content.size shouldBe 3
                    result.totalElements shouldBe 3
                    result.pageNumber shouldBe 1
                }
            }

            When("현장이 없으면") {
                val page = PageImpl(emptyList<Site>())

                every {
                    siteRepository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Site>>>(),
                    )
                } returns page

                every { fileService.getFiles(any()) } returns emptyList()

                val result = service.findAll(PageSearchRequest(page = 1, size = 10))

                Then("빈 결과가 반환된다") {
                    result.content.size shouldBe 0
                    result.totalElements shouldBe 0
                }
            }
        }

        Given("현장 수정") {

            When("썸네일 이미지를 변경하면") {
                val site = dummySite(id = 1L, thumbnailImageId = 10L)
                val request = dummySiteRequest(name = "서울역 현장 (수정)", region = Region.GYEONGGI_INCHEON, thumbnailImageId = 20L)

                every { siteRepository.findByIdOrNull(1L) } returns site

                service.update(1L, request)

                Then("현장 정보가 수정된다") {
                    site.name shouldBe "서울역 현장 (수정)"
                    site.region shouldBe Region.GYEONGGI_INCHEON
                    site.thumbnailImageId shouldBe 20L
                }

                Then("새 썸네일 파일 업로드가 확정된다") {
                    verify { fileService.finalizeUpload(20L, "sites/1/") }
                }
            }

            When("썸네일 이미지를 변경하지 않으면") {
                val site = dummySite(id = 1L, thumbnailImageId = 10L)
                val request = dummySiteRequest(thumbnailImageId = 10L)

                every { siteRepository.findByIdOrNull(1L) } returns site

                service.update(1L, request)

                Then("파일 업로드가 수행되지 않는다") {
                    verify(exactly = 0) { fileService.finalizeUpload(any(), eq("sites/1/")) }
                }
            }

            When("존재하지 않는 현장을 수정하면") {
                val request = dummySiteRequest()

                every { siteRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.update(999L, request)
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_SITE
                }
            }
        }

        Given("현장 삭제") {

            When("존재하는 현장을 삭제하면") {
                val site = dummySite(id = 1L)

                every { siteRepository.findByIdOrNull(1L) } returns site

                service.delete(1L)

                Then("현장이 삭제된다") {
                    verify { siteRepository.delete(site) }
                }
            }

            When("존재하지 않는 현장을 삭제하면") {
                every { siteRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.delete(999L)
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_SITE
                }
            }
        }
    })
