package com.pluxity.safers.event.kafka

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.event.dto.dummyEventCreateRequest
import com.pluxity.safers.event.service.EventFacade
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class CctvEventConsumerTest :
    BehaviorSpec({

        val eventFacade: EventFacade = mockk()
        val consumer = CctvEventConsumer(eventFacade)

        Given("이벤트 소비") {

            When("이벤트 메시지를 소비하면") {
                val request = dummyEventCreateRequest()
                every { eventFacade.create(any()) } returns 1L

                consumer.consumeEvent(request)

                Then("EventFacade.create가 호출된다") {
                    verify { eventFacade.create(request) }
                }
            }
        }

        Given("영상 소비") {

            When("영상 메시지를 소비하면") {
                val message = CctvVideoMessage(eventId = 1L, videoUrl = "http://localhost:8080/videos/clip.mp4")
                every { eventFacade.uploadVideo(any(), any()) } just runs

                consumer.consumeVideo(message)

                Then("EventFacade.uploadVideo가 호출된다") {
                    verify { eventFacade.uploadVideo(1L, "http://localhost:8080/videos/clip.mp4") }
                }
            }
        }
    })
