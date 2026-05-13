package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.queue.ForwardQueue
import com.pluxity.safersCollect.v1.collect.adapter.BandTelemetryAdapter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class BandTelemetryMqttHandlerTest :
    BehaviorSpec({

        val objectMapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        val adapter = BandTelemetryAdapter()
        val queue = mockk<ForwardQueue>(relaxed = true)
        val handler = BandTelemetryMqttHandler(objectMapper, adapter, queue)

        Given("BandTelemetry 1건 JSON 페이로드") {
            val json =
                """
                {
                  "bandId": "BAND-A1B2C3",
                  "timestamp": "2026-04-24T10:15:30",
                  "vitals": {"heartRateBpm": 88, "bodyTempC": 36.7, "spo2": 97},
                  "battery": 73,
                  "wearState": "WORN"
                }
                """.trimIndent().toByteArray()

            When("handle 호출") {
                handler.handle(json)

                Then("topicSuffix 는 band/telemetry") {
                    handler.topicSuffix shouldBe "band/telemetry"
                }

                Then("ForwardQueue.enqueueAll 이 envelope 1개 collection 으로 호출됨") {
                    val captured = slot<Collection<TelemetryEnvelope>>()
                    verify(exactly = 1) { queue.enqueueAll(capture(captured)) }
                    captured.captured.size shouldBe 1
                    captured.captured.first().sourceId shouldBe "BAND-A1B2C3"
                }
            }
        }
    })
