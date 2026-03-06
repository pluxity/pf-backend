package com.pluxity.safers.event.service

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.file.service.FileService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class EventFileDownloadServiceTest :
    BehaviorSpec({

        val fileService: FileService = mockk(relaxed = true)
        val webClientFactory: WebClientFactory = mockk()
        val service = EventFileDownloadService(fileService, webClientFactory)

        Given("파일 다운로드 및 업로드") {

            When("정상적인 URL로 파일을 다운로드하면") {
                val fileBytes = "test-content".toByteArray()
                val requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*> = mockk()
                val requestHeadersSpec: WebClient.RequestHeadersSpec<*> = mockk()
                val responseSpec: WebClient.ResponseSpec = mockk()
                val webClient: WebClient = mockk()

                every { webClientFactory.createClient("https://example.com") } returns webClient
                every { webClient.get() } returns requestHeadersUriSpec
                every { requestHeadersUriSpec.uri("/path/image.jpg") } returns requestHeadersSpec
                every { requestHeadersSpec.retrieve() } returns responseSpec
                every { responseSpec.bodyToMono(any<ParameterizedTypeReference<ByteArray>>()) } returns Mono.just(fileBytes)
                every { fileService.initiateUpload(fileBytes, "image.jpg", "image/jpeg") } returns 10L

                val result = service.downloadAndInitiateUpload("https://example.com/path/image.jpg")

                Then("파일 ID가 반환된다") {
                    result shouldBe 10L
                }

                Then("올바른 content type으로 업로드된다") {
                    verify { fileService.initiateUpload(fileBytes, "image.jpg", "image/jpeg") }
                }
            }

            When("다운로드 중 예외가 발생하면") {
                val webClient: WebClient = mockk()
                val requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*> = mockk()
                val requestHeadersSpec: WebClient.RequestHeadersSpec<*> = mockk()
                val responseSpec: WebClient.ResponseSpec = mockk()

                every { webClientFactory.createClient("https://fail.com") } returns webClient
                every { webClient.get() } returns requestHeadersUriSpec
                every { requestHeadersUriSpec.uri("/error/file.png") } returns requestHeadersSpec
                every { requestHeadersSpec.retrieve() } returns responseSpec
                every { responseSpec.bodyToMono(any<ParameterizedTypeReference<ByteArray>>()) } returns
                    Mono.error(RuntimeException("connection failed"))

                val result = service.downloadAndInitiateUpload("https://fail.com/error/file.png")

                Then("null이 반환된다") {
                    result.shouldBeNull()
                }
            }

            When("mp4 파일을 다운로드하면") {
                val fileBytes = "video-content".toByteArray()
                val webClient: WebClient = mockk()
                val requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*> = mockk()
                val requestHeadersSpec: WebClient.RequestHeadersSpec<*> = mockk()
                val responseSpec: WebClient.ResponseSpec = mockk()

                every { webClientFactory.createClient("https://example.com") } returns webClient
                every { webClient.get() } returns requestHeadersUriSpec
                every { requestHeadersUriSpec.uri("/videos/clip.mp4") } returns requestHeadersSpec
                every { requestHeadersSpec.retrieve() } returns responseSpec
                every { responseSpec.bodyToMono(any<ParameterizedTypeReference<ByteArray>>()) } returns Mono.just(fileBytes)
                every { fileService.initiateUpload(fileBytes, "clip.mp4", "video/mp4") } returns 20L

                val result = service.downloadAndInitiateUpload("https://example.com/videos/clip.mp4")

                Then("video/mp4 content type으로 업로드된다") {
                    result shouldBe 20L
                    verify { fileService.initiateUpload(fileBytes, "clip.mp4", "video/mp4") }
                }
            }
        }
    })
