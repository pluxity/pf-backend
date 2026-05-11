package com.pluxity.safersCollect.queue

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.forwarder.enums.SourceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.LocalDateTime

class ForwardQueueTest :
    BehaviorSpec({

        fun props(capacity: Int) =
            SafersCollectProperties(
                site = SafersCollectProperties.Site(id = 0L),
                central = SafersCollectProperties.Central(ingestUrl = "http://test", apiKey = "test"),
                queue = SafersCollectProperties.Queue(normalCapacity = capacity, dropPolicy = DropPolicy.DROP_OLDEST),
                forwarder =
                    SafersCollectProperties.Forwarder(
                        batchSize = 100,
                        flushIntervalMs = 1000,
                    ),
            )

        fun queueOf(capacity: Int): ForwardQueue {
            val p = props(capacity)
            return ForwardQueue(p, BackpressurePolicy(p, SimpleMeterRegistry()))
        }

        fun env(id: String) =
            TelemetryEnvelope(
                sourceType = SourceType.GAS,
                sourceId = id,
                timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                measurement = "gas_reading",
            )

        Given("빈 queue") {
            val queue = queueOf(10)

            Then("size 는 0") {
                queue.size() shouldBe 0
            }

            When("drain(5) 호출") {
                val drained = queue.drain(5)

                Then("emptyList 반환 — block 안 함") {
                    drained shouldBe emptyList()
                }
            }
        }

        Given("envelope 5개를 enqueue 한 queue (capacity 10)") {
            val queue = queueOf(10)
            queue.enqueueAll((1..5).map { env("$it") })

            Then("size 는 5") {
                queue.size() shouldBe 5
            }

            When("drain(3) — drain less than size") {
                val drained = queue.drain(3)

                Then("oldest 3개를 순서대로 반환 + size 는 2 로 줄어듦") {
                    drained.map { it.sourceId } shouldBe listOf("1", "2", "3")
                    queue.size() shouldBe 2
                }
            }
        }

        Given("envelope 3개를 enqueue 한 queue (capacity 10)") {
            val queue = queueOf(10)
            queue.enqueueAll((1..3).map { env("$it") })

            When("drain(10) — drain more than size") {
                val drained = queue.drain(10)

                Then("있는 3개 전부 반환 + size 0 — block 없이 즉시") {
                    drained shouldHaveSize 3
                    drained.map { it.sourceId } shouldBe listOf("1", "2", "3")
                    queue.size() shouldBe 0
                }
            }
        }

        Given("capacity 5 queue 에 envelope 7개를 enqueue (overflow)") {
            val queue = queueOf(5)
            queue.enqueueAll((1..7).map { env("$it") })

            Then("size 는 capacity 인 5 로 제한됨 (drop-oldest 정책 적용)") {
                queue.size() shouldBe 5
            }

            When("drain(10)") {
                val drained = queue.drain(10)

                Then("oldest 2개 (1,2) 가 폐기되고 마지막 5개 (3..7) 만 남아있음") {
                    drained.map { it.sourceId } shouldBe listOf("3", "4", "5", "6", "7")
                }
            }
        }

        Given("queue 재사용 시나리오 — enqueue → drain → enqueue") {
            val queue = queueOf(10)
            queue.enqueueAll((1..3).map { env("$it") })
            queue.drain(10) // 첫 batch 비움
            queue.enqueueAll((4..6).map { env("$it") })

            When("두 번째 drain") {
                val drained = queue.drain(10)

                Then("두 번째 enqueue 한 것만 반환 — 첫 batch 와 섞이지 않음") {
                    drained.map { it.sourceId } shouldBe listOf("4", "5", "6")
                }
            }
        }
    })
