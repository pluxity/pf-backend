package com.pluxity.safers.event.service

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.common.file.service.FileService
import com.pluxity.common.test.dto.dummyFileResponse
import com.pluxity.safers.event.dto.dummyEventCreateRequest
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.dummyEvent
import com.pluxity.safers.event.listener.EventCreated
import com.pluxity.safers.event.listener.EventVideoRegistered
import com.pluxity.safers.event.repository.EventRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer

class EventServiceTest :
    BehaviorSpec({

        mockkStatic("com.pluxity.common.core.utils.KotlinJdslExtensionsKt")

        val eventRepository: EventRepository = mockk(relaxed = true)
        val fileService: FileService = mockk(relaxed = true)
        val eventFileDownloadService: EventFileDownloadService = mockk()
        val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
        val transactionTemplate: TransactionTemplate = mockk()

        val service =
            EventService(
                eventRepository,
                fileService,
                eventFileDownloadService,
                eventPublisher,
                transactionTemplate,
            )

        every { transactionTemplate.execute(any<TransactionCallback<Long>>()) } answers {
            firstArg<TransactionCallback<Long>>().doInTransaction(mockk())
        }

        every { transactionTemplate.executeWithoutResult(any()) } answers {
            firstArg<Consumer<TransactionStatus>>().accept(mockk())
        }

        Given("이벤트 생성") {

            When("스냅샷 파일이 포함된 이벤트를 생성하면") {
                val request = dummyEventCreateRequest()
                val savedEvent = dummyEvent(id = 1L)
                val snapshotFileId = 10L
                val snapshotFileResponse = dummyFileResponse(id = snapshotFileId)

                every { eventFileDownloadService.downloadAndInitiateUpload("/snapshots/", request.snapshot) } returns snapshotFileId
                every { eventRepository.save(any()) } returns savedEvent
                every { fileService.getFileResponse(snapshotFileId) } returns snapshotFileResponse

                val result = service.create(request)

                Then("이벤트 ID가 반환된다") {
                    result shouldBe 1L
                }

                Then("파일 업로드가 확정된다") {
                    verify { fileService.finalizeUpload(snapshotFileId, "events/1/") }
                }

                Then("EventCreated 이벤트가 발행된다") {
                    verify { eventPublisher.publishEvent(any<EventCreated>()) }
                }
            }

            When("스냅샷 파일 다운로드에 실패하면") {
                val request = dummyEventCreateRequest()
                val savedEvent = dummyEvent(id = 2L)

                every { eventFileDownloadService.downloadAndInitiateUpload("/snapshots/", request.snapshot) } returns null
                every { eventRepository.save(any()) } returns savedEvent
                every { fileService.getFileResponse(null) } returns null

                val result = service.create(request)

                Then("이벤트는 저장되지만 파일 업로드는 수행되지 않는다") {
                    result shouldBe 2L
                    verify(exactly = 0) { fileService.finalizeUpload(any(), any()) }
                }
            }
        }

        Given("이벤트 영상 등록") {

            When("존재하는 이벤트에 영상을 등록하면") {
                val event = dummyEvent(id = 1L, snapshotFileId = 10L)
                val videoFileId = 20L
                val snapshotFileResponse = dummyFileResponse(id = 10L)
                val videoFileResponse = dummyFileResponse(id = videoFileId, originalFileName = "video.mp4", contentType = "video/mp4")

                every { eventFileDownloadService.downloadAndInitiateUpload("/videos/", "video.mp4") } returns videoFileId
                every { eventRepository.findByIdOrNull(1L) } returns event
                every { fileService.finalizeUpload(videoFileId, "events/1/") } returns mockk()
                every { fileService.getFileResponse(10L) } returns snapshotFileResponse
                every { fileService.getFileResponse(videoFileId) } returns videoFileResponse

                service.uploadVideo(1L, "video.mp4")

                Then("영상 파일이 할당된다") {
                    event.videoFileId shouldBe videoFileId
                }

                Then("파일 업로드가 확정된다") {
                    verify { fileService.finalizeUpload(videoFileId, "events/1/") }
                }

                Then("EventVideoRegistered 이벤트가 발행된다") {
                    verify { eventPublisher.publishEvent(any<EventVideoRegistered>()) }
                }
            }

            When("존재하지 않는 이벤트에 영상을 등록하면") {
                every { eventFileDownloadService.downloadAndInitiateUpload("/videos/", "video.mp4") } returns 20L
                every { eventRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.uploadVideo(999L, "video.mp4")
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_EVENT
                }
            }

            When("영상 파일 다운로드에 실패하면") {
                val event = dummyEvent(id = 1L)

                every { eventFileDownloadService.downloadAndInitiateUpload("/videos/", "video.mp4") } returns null
                every { eventRepository.findByIdOrNull(1L) } returns event

                service.uploadVideo(1L, "video.mp4")

                Then("파일 업로드가 수행되지 않는다") {
                    verify(exactly = 0) { fileService.finalizeUpload(any(), eq("events/1/")) }
                }

                Then("EventVideoRegistered 이벤트가 발행되지 않는다") {
                    verify(exactly = 0) { eventPublisher.publishEvent(any<EventVideoRegistered>()) }
                }
            }
        }

        Given("이벤트 단건 조회") {

            When("존재하는 이벤트를 조회하면") {
                val event = dummyEvent(id = 1L, snapshotFileId = 10L, videoFileId = 20L)
                val snapshotFileResponse = dummyFileResponse(id = 10L)
                val videoFileResponse = dummyFileResponse(id = 20L, originalFileName = "video.mp4", contentType = "video/mp4")

                every { eventRepository.findByIdOrNull(1L) } returns event
                every { fileService.getFileResponse(10L) } returns snapshotFileResponse
                every { fileService.getFileResponse(20L) } returns videoFileResponse

                val result = service.findById(1L)

                Then("이벤트 정보가 반환된다") {
                    result.id shouldBe 1L
                    result.eventId shouldBe "EVT-20240101-001"
                    result.name shouldBe "헬멧 미착용 감지"
                    result.snapshot shouldBe snapshotFileResponse
                    result.video shouldBe videoFileResponse
                }
            }

            When("존재하지 않는 이벤트를 조회하면") {
                every { eventRepository.findByIdOrNull(999L) } returns null

                Then("CustomException이 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.findById(999L)
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_EVENT
                }
            }
        }

        Given("이벤트 전체 조회") {

            When("이벤트 목록을 조회하면") {
                val events =
                    listOf(
                        dummyEvent(id = 1L, name = "이벤트 1", snapshotFileId = 10L),
                        dummyEvent(id = 2L, name = "이벤트 2", snapshotFileId = 11L),
                        dummyEvent(id = 3L, name = "이벤트 3", snapshotFileId = 12L),
                    )

                val page = PageImpl(events)

                every {
                    eventRepository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Event>>>(),
                    )
                } returns page

                val fileResponse10 = dummyFileResponse(id = 10L)
                val fileResponse11 = dummyFileResponse(id = 11L)
                val fileResponse12 = dummyFileResponse(id = 12L)
                every { fileService.getFiles(any()) } returns listOf(fileResponse10, fileResponse11, fileResponse12)

                val result = service.findAll(PageSearchRequest(page = 1, size = 10))

                Then("페이징된 결과가 반환된다") {
                    result.content.size shouldBe 3
                    result.totalElements shouldBe 3
                    result.pageNumber shouldBe 1
                }
            }

            When("이벤트가 없으면") {
                val page = PageImpl(emptyList<Event>())

                every {
                    eventRepository.findPageNotNull(
                        any<Pageable>(),
                        any<Jpql.() -> JpqlQueryable<SelectQuery<Event>>>(),
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
    })
