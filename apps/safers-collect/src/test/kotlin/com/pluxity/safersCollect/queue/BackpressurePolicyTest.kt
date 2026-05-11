package com.pluxity.safersCollect.queue

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import com.pluxity.safersCollect.forwarder.enums.SourceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue

class BackpressurePolicyTest :
    BehaviorSpec({

        fun props(policy: DropPolicy) =
            SafersCollectProperties(
                site = SafersCollectProperties.Site(id = 0L),
                central = SafersCollectProperties.Central(ingestUrl = "http://test", apiKey = "test"),
                queue = SafersCollectProperties.Queue(normalCapacity = 3, dropPolicy = policy),
                forwarder =
                    SafersCollectProperties.Forwarder(
                        batchSize = 100,
                        flushIntervalMs = 1000,
                    ),
            )

        fun env(id: String) =
            TelemetryEnvelope(
                sourceType = SourceType.GAS,
                sourceId = id,
                timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                measurement = "gas_reading",
            )

        Given("DROP_OLDEST 정책 + 가득 찬 큐") {
            val backpressure = BackpressurePolicy(props(DropPolicy.DROP_OLDEST), SimpleMeterRegistry())
            val queue = LinkedBlockingQueue<TelemetryEnvelope>(3)
            queue.offer(env("1"))
            queue.offer(env("2"))
            queue.offer(env("3"))

            When("onOverflow 로 새 envelope 추가") {
                backpressure.onOverflow(queue, env("4"))

                Then("oldest(1) 가 폐기되고 새 envelope 가 tail 에 들어감") {
                    queue.toList().map { it.sourceId } shouldBe listOf("2", "3", "4")
                }

                Then("size 는 capacity 그대로 유지") {
                    queue.size shouldBe 3
                }

                Then("dropped 카운터가 1 증가") {
                    backpressure.droppedTotal() shouldBe 1L
                }
            }
        }

        Given("DROP_OLDEST 정책 + 빈 큐 (overflow 가 아닌 비정상 호출)") {
            val backpressure = BackpressurePolicy(props(DropPolicy.DROP_OLDEST), SimpleMeterRegistry())
            val queue = LinkedBlockingQueue<TelemetryEnvelope>(3)

            When("onOverflow 호출") {
                backpressure.onOverflow(queue, env("X"))

                Then("poll 은 null 반환하고 offer 는 성공 — 큐에 1개만 남음") {
                    queue.toList().map { it.sourceId } shouldBe listOf("X")
                }

                Then("dropped 카운터는 호출 자체로 1 증가 (비정상 호출도 drop 시도로 집계)") {
                    backpressure.droppedTotal() shouldBe 1L
                }
            }
        }

        Given("연속 overflow 시나리오 — 큐 가득 + onOverflow 5회 호출") {
            val backpressure = BackpressurePolicy(props(DropPolicy.DROP_OLDEST), SimpleMeterRegistry())
            val queue = LinkedBlockingQueue<TelemetryEnvelope>(3)
            queue.offer(env("a"))
            queue.offer(env("b"))
            queue.offer(env("c"))

            When("onOverflow(d), onOverflow(e), onOverflow(f), onOverflow(g), onOverflow(h)") {
                listOf("d", "e", "f", "g", "h").forEach {
                    backpressure.onOverflow(queue, env(it))
                }

                Then("dropped 카운터가 5 누적") {
                    backpressure.droppedTotal() shouldBe 5L
                }

                Then("최신 3개만 큐에 남음") {
                    queue.toList().map { it.sourceId } shouldBe listOf("f", "g", "h")
                }
            }
        }
    })
