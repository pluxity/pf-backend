package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.v1.collect.adapter.SosEventAdapter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class SosEventMqttHandlerTest :
    BehaviorSpec({

        val objectMapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        val adapter = SosEventAdapter()
        val eventForwarder = mockk<EventForwarder>(relaxed = true)
        val handler = SosEventMqttHandler(objectMapper, adapter, eventForwarder)

        Given("SOS BUTTON_LONG_PRESS 이벤트 JSON 페이로드") {
            val json =
                """
                {
                  "eventId": "SOS-EVT-20260424-0001",
                  "severity": "CRITICAL",
                  "occurredAt": "2026-04-24T10:15:30",
                  "rawPosition": {"lat": 37.5, "lon": 127.0},
                  "payload": {
                    "bandId": "BAND-A1B2C3",
                    "trigger": "BUTTON_LONG_PRESS"
                  }
                }
                """.trimIndent().toByteArray()

            When("handle 호출") {
                handler.handle(json)

                Then("topicSuffix 는 sos/events") {
                    handler.topicSuffix shouldBe "sos/events"
                }

                Then("EventForwarder.forward 가 envelope 1개로 호출됨") {
                    val captured = slot<EventEnvelope>()
                    verify(exactly = 1) { eventForwarder.forward(capture(captured)) }
                    captured.captured.eventId shouldBe "SOS-EVT-20260424-0001"
                    captured.captured.bandId shouldBe "BAND-A1B2C3"
                }
            }
        }
    })
