package com.pluxity.safersCollect.forwarder

import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.enums.EventSeverity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class EventForwarderTest :
    BehaviorSpec({

        fun envelope() =
            EventEnvelope(
                eventId = "SOS-EVT-001",
                eventType = WireEventType.SOS_TRIGGERED,
                severity = EventSeverity.CRITICAL,
                source = WireEventSource.SOS_DEVICE,
                occurredAt = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                bandId = "BAND-A1B2C3",
                payload = mapOf("trigger" to "BUTTON_LONG_PRESS"),
            )

        Given("client 가 정상 응답") {
            val client = mockk<CentralIngestClient>(relaxed = true)
            val forwarder = EventForwarder(client)

            When("forward 호출") {
                val env = envelope()
                forwarder.forward(env)

                Then("client.postEvent 가 envelope 그대로 호출") {
                    verify(exactly = 1) { client.postEvent(env) }
                }
            }
        }

        Given("client 가 예외를 던지는 상황") {
            val client = mockk<CentralIngestClient>()
            every { client.postEvent(any()) } throws RuntimeException("safers down")

            val forwarder = EventForwarder(client)

            When("forward 호출") {
                Then("예외가 그대로 propagate (phase 1 — vendor retry 위임)") {
                    shouldThrow<RuntimeException> {
                        forwarder.forward(envelope())
                    }
                }
            }
        }
    })
