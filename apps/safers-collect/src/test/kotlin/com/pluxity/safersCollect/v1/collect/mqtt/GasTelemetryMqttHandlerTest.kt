package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.queue.ForwardQueue
import com.pluxity.safersCollect.v1.collect.adapter.GasTelemetryAdapter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class GasTelemetryMqttHandlerTest :
    BehaviorSpec({

        val objectMapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        val adapter = GasTelemetryAdapter()
        val queue = mockk<ForwardQueue>(relaxed = true)
        val handler = GasTelemetryMqttHandler(objectMapper, adapter, queue)

        Given("GasTelemetry 1건 JSON 페이로드") {
            val json =
                """
                {
                  "deviceId": "GAS-MH203-01",
                  "timestamp": "2026-04-24T10:15:30",
                  "measurements": [
                    {"gas": "O2", "value": 3.1, "unit": "PPM"}
                  ],
                  "battery": 87,
                  "signalRssi": -68
                }
                """.trimIndent().toByteArray()

            When("handle 호출") {
                handler.handle(json)

                Then("topicSuffix 는 gas/telemetry") {
                    handler.topicSuffix shouldBe "gas/telemetry"
                }

                Then("ForwardQueue.enqueueAll 이 envelope 1개 collection 으로 호출됨") {
                    val captured = slot<Collection<TelemetryEnvelope>>()
                    verify(exactly = 1) { queue.enqueueAll(capture(captured)) }
                    captured.captured.size shouldBe 1
                    captured.captured.first().sourceId shouldBe "GAS-MH203-01"
                }
            }
        }
    })
