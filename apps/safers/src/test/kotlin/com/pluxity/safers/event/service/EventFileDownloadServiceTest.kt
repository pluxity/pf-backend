package com.pluxity.safers.event.service

import com.pluxity.common.file.service.FileService
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.InetSocketAddress

class EventFileDownloadServiceTest :
    BehaviorSpec({

        val fileService: FileService = mockk(relaxed = true)
        val service = EventFileDownloadService(fileService)

        lateinit var server: HttpServer
        var port = 0

        beforeSpec {
            server =
                HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                    createContext("/path/image.jpg") { exchange ->
                        val body = "test-content".toByteArray()
                        exchange.sendResponseHeaders(200, body.size.toLong())
                        exchange.responseBody.use { it.write(body) }
                    }
                    createContext("/videos/clip.mp4") { exchange ->
                        val body = "video-content".toByteArray()
                        exchange.sendResponseHeaders(200, body.size.toLong())
                        exchange.responseBody.use { it.write(body) }
                    }
                    createContext("/error/file.png") { exchange ->
                        exchange.sendResponseHeaders(500, -1)
                        exchange.close()
                    }
                    start()
                }
            port = server.address.port
        }

        afterSpec {
            server.stop(0)
        }

        Given("파일 다운로드 및 업로드") {

            When("정상적인 URL로 파일을 다운로드하면") {
                every { fileService.initiateUpload(any(), "image.jpg", "image/jpeg") } returns 10L

                val result = service.downloadAndInitiateUpload("http://127.0.0.1:$port/path/image.jpg")

                Then("파일 ID가 반환된다") {
                    result shouldBe 10L
                }

                Then("올바른 content type으로 업로드된다") {
                    verify {
                        fileService.initiateUpload(
                            match { String(it) == "test-content" },
                            "image.jpg",
                            "image/jpeg",
                        )
                    }
                }
            }

            When("다운로드 중 예외가 발생하면") {
                val result = service.downloadAndInitiateUpload("http://127.0.0.1:$port/error/file.png")

                Then("null이 반환된다") {
                    result.shouldBeNull()
                }
            }

            When("스킴 또는 authority가 없는 URL을 전달하면") {
                val result = service.downloadAndInitiateUpload("not-a-valid-url")

                Then("null이 반환된다") {
                    result.shouldBeNull()
                }
            }

            When("허용되지 않는 스킴(file)의 URL을 전달하면") {
                val result = service.downloadAndInitiateUpload("file:///etc/passwd")

                Then("null이 반환된다") {
                    result.shouldBeNull()
                }
            }

            When("mp4 파일을 다운로드하면") {
                every { fileService.initiateUpload(any(), "clip.mp4", "video/mp4") } returns 20L

                val result = service.downloadAndInitiateUpload("http://127.0.0.1:$port/videos/clip.mp4")

                Then("video/mp4 content type으로 업로드된다") {
                    result shouldBe 20L
                    verify {
                        fileService.initiateUpload(
                            match { String(it) == "video-content" },
                            "clip.mp4",
                            "video/mp4",
                        )
                    }
                }
            }
        }
    })
