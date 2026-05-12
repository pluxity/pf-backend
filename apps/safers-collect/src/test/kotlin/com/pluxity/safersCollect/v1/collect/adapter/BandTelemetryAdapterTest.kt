package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.v1.collect.dto.BandCollectRequest
import com.pluxity.safersCollect.v1.collect.dto.BandVitals
import com.pluxity.safersCollect.v1.collect.dto.RawPosition
import com.pluxity.safersCollect.v1.collect.enums.WearState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class BandTelemetryAdapterTest :
    BehaviorSpec({

        val adapter = BandTelemetryAdapter()

        Given("rawPosition / vitals / wearState / battery 모두 채워진 sample") {
            val request =
                BandCollectRequest(
                    bandId = "BAND-A1B2C3",
                    timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                    rawPosition =
                        RawPosition(
                            lat = 37.5665,
                            lon = 126.9780,
                            accuracyM = 3.5,
                        ),
                    vitals =
                        BandVitals(
                            heartRateBpm = 70,
                            bodyTempC = 36.7,
                            spo2 = 97,
                            step = 4321,
                        ),
                    battery = 100,
                    wearState = WearState.WORN,
                )

            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("envelope 1개 — Band 는 fan-out 안 함") {
                    envelopes.size shouldBe 1
                }
                Then("fields 에 lat/lon/accuracy_m/heart_rate_bpm/body_temp_c/spo2/step/battery 8개 모두") {
                    envelopes.first().fields.keys shouldBe
                        setOf(
                            "lat",
                            "lon",
                            "accuracy_m",
                            "heart_rate_bpm",
                            "body_temp_c",
                            "spo2",
                            "step",
                            "battery",
                        )
                }
                Then("tag.wear_state 가 enum.name 으로 들어감") {
                    envelopes.first().tags["wear_state"] shouldBe "WORN"
                }
            }
        }

        Given("rawPosition.accuracyM 만 null") {
            val request =
                BandCollectRequest(
                    bandId = "BAND-A1B2C3",
                    timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                    rawPosition =
                        RawPosition(
                            lat = 37.5665,
                            lon = 126.9780,
                            accuracyM = null,
                        ),
                    vitals = null,
                    battery = 100,
                    wearState = WearState.WORN,
                )
            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("fields 에 lat/lon 은 있고 accuracy_m 는 없음") {
                    envelopes.first().fields.keys shouldBe setOf("lat", "lon", "battery")
                }
            }
        }

        Given("optional 필드 전부 null 인 최소 sample") {
            val request =
                BandCollectRequest(
                    bandId = "BAND-MIN",
                    timestamp = LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                    rawPosition = null,
                    vitals = null,
                    battery = null,
                    wearState = null,
                )

            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("envelope 1개, sourceId 는 bandId") {
                    envelopes.size shouldBe 1
                    envelopes.first().sourceId shouldBe "BAND-MIN"
                }
                Then("fields/tags 가 비어 있음 — null 키는 모두 누락") {
                    envelopes.first().fields shouldBe emptyMap()
                    envelopes.first().tags shouldBe mapOf("band_id" to "BAND-MIN")
                }
            }
        }
    })
