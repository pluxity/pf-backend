package com.pluxity.safers.collect.service

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventVideoUploadRequest
import com.pluxity.safers.event.dto.dummyEventCreateRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.kafka.core.KafkaTemplate
import java.util.concurrent.CompletableFuture

class CctvEventCollectorTest :
    BehaviorSpec({

        val eventKafkaTemplate: KafkaTemplate<String, EventCreateRequest> = mockk()
        val videoKafkaTemplate: KafkaTemplate<String, CctvVideoMessage> = mockk()

        val collector = CctvEventCollector(eventKafkaTemplate, videoKafkaTemplate)

        Given("이벤트 수집 발행") {

            When("유효한 이벤트를 발행하면") {
                val request = dummyEventCreateRequest()
                every { eventKafkaTemplate.send(any<String>(), any(), any()) } returns CompletableFuture.completedFuture(mockk())

                collector.collect(request)

                Then("올바른 토픽과 키로 발행된다") {
                    verify {
                        eventKafkaTemplate.send(
                            CctvEventCollector.TOPIC_EVENTS,
                            request.eventId,
                            request,
                        )
                    }
                }
            }
        }

        Given("영상 수집 발행") {

            When("영상 URL을 발행하면") {
                val eventId = "EVT-20260101-001"
                val request = EventVideoUploadRequest(video = "http://localhost:8080/videos/clip.mp4")
                every { videoKafkaTemplate.send(any<String>(), any(), any()) } returns CompletableFuture.completedFuture(mockk())

                collector.collectVideo(eventId, request)

                Then("올바른 토픽과 키로 발행된다") {
                    verify {
                        videoKafkaTemplate.send(
                            CctvEventCollector.TOPIC_VIDEOS,
                            eventId,
                            match<CctvVideoMessage> { it.eventId == eventId && it.videoUrl == request.video },
                        )
                    }
                }
            }
        }
    })
