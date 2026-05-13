package com.pluxity.safersCollect.forwarder

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.forwarder.enums.SourceType
import com.pluxity.safersCollect.queue.DropPolicy
import com.pluxity.safersCollect.queue.ForwardQueue
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class TelemetryForwarderTest :
    BehaviorSpec({

        fun props() =
            SafersCollectProperties(
                site = SafersCollectProperties.Site(id = 42L),
                central = SafersCollectProperties.Central(ingestUrl = "http://test", apiKey = "test"),
                queue = SafersCollectProperties.Queue(normalCapacity = 1000, dropPolicy = DropPolicy.DROP_OLDEST),
                forwarder =
                    SafersCollectProperties.Forwarder(
                        batchSize = 100,
                        flushIntervalMs = 1000,
                    ),
                mqtt =
                    SafersCollectProperties.Mqtt(
                        host = "localhost",
                        port = 1883,
                        clientId = "test",
                        topicPrefix = "safers/collect",
                    ),
            )

        fun env(id: String) =
            TelemetryEnvelope(
                sourceType = SourceType.GAS,
                sourceId = id,
                timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                measurement = "gas_reading",
            )

        Given("queue 가 비어있을 때") {
            val queue = mockk<ForwardQueue>(relaxed = true)
            val client = mockk<CentralIngestClient>(relaxed = true)
            every { queue.drain(any()) } returns emptyList()

            val forwarder = TelemetryForwarder(queue, client, props())

            When("flush 호출") {
                forwarder.flush()

                Then("client 호출 없이 즉시 반환") {
                    verify(exactly = 0) { client.postTelemetry(any()) }
                }
            }
        }

        Given("queue 에 envelope 3개 + central 정상") {
            val queue = mockk<ForwardQueue>(relaxed = true)
            val client = mockk<CentralIngestClient>(relaxed = true)
            val batch = listOf(env("1"), env("2"), env("3"))
            every { queue.drain(any()) } returns batch andThen emptyList()

            val forwarder = TelemetryForwarder(queue, client, props())

            When("flush 호출") {
                forwarder.flush()

                Then("drain 한 batch 가 그대로 client 로 forward 됨") {
                    verify(exactly = 1) { client.postTelemetry(batch) }
                }
                Then("재enqueue 발생 안 함 (정상 경로)") {
                    verify(exactly = 0) { queue.enqueueAll(any()) }
                }
            }
        }

        Given("client 가 예외를 던지는 상황") {
            val queue = mockk<ForwardQueue>(relaxed = true)
            val client = mockk<CentralIngestClient>()
            val batch = listOf(env("1"), env("2"))
            every { queue.drain(any()) } returns batch
            every { client.postTelemetry(any()) } throws RuntimeException("central down")

            val forwarder = TelemetryForwarder(queue, client, props())

            When("flush 호출") {
                forwarder.flush()

                Then("실패한 batch 를 queue 로 재enqueue — 다음 주기에 자연 재시도") {
                    verify(exactly = 1) { queue.enqueueAll(batch) }
                }
            }
        }
    })
