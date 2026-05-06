package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.v1.collect.dto.BandCollectRequest
import com.pluxity.safersCollect.v1.collect.dto.BandSample
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
                    samples =
                        listOf(
                            BandSample(
                                bandId = "BAND-A1B2C3",
                                timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                                rawPosition =
                                    RawPosition(
                                        lat = 37.5665,
                                        lng = 126.9780,
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
                            ),
                        ),
                )

            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("envelope 1개 — Band 는 fan-out 안 함") {
                    envelopes.size shouldBe 1
                }
                Then("fields 에 lat/lng/accuracy_m/heart_rate_bpm/body_temp_c/spo2/step/battery 8개 모두") {
                    envelopes.first().fields.keys shouldBe
                        setOf(
                            "lat",
                            "lng",
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
                    samples =
                        listOf(
                            BandSample(
                                bandId = "BAND-A1B2C3",
                                timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30),
                                rawPosition =
                                    RawPosition(
                                        lat = 37.5665,
                                        lng = 126.9780,
                                        accuracyM = null,
                                    ),
                                vitals = null,
                                battery = 100,
                                wearState = WearState.WORN,
                            ),
                        ),
                )
            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("fields 에 lat/lng 은 있고 accuracy_m 는 없음") {
                    envelopes.first().fields.keys shouldBe setOf("lat", "lng", "battery")
                }
            }
        }

        Given("samples 3건 (각각 다른 bandId)") {
            val request =
                BandCollectRequest(
                    samples =
                        listOf(
                            BandSample(
                                bandId = "BAND-01",
                                timestamp = LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                                rawPosition = null,
                                vitals = null,
                                battery = null,
                                wearState = null,
                            ),
                            BandSample(
                                bandId = "BAND-02",
                                timestamp = LocalDateTime.of(2026, 2, 1, 0, 0, 0),
                                rawPosition = null,
                                vitals = null,
                                battery = null,
                                wearState = null,
                            ),
                            BandSample(
                                bandId = "BAND-03",
                                timestamp = LocalDateTime.of(2026, 3, 1, 0, 0, 0),
                                rawPosition = null,
                                vitals = null,
                                battery = null,
                                wearState = null,
                            ),
                        ),
                )

            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("envelope 3개 — Gas 와 달리 fan-out 없이 1:1") {
                    envelopes.size shouldBe 3
                    envelopes.map { it.sourceId } shouldBe listOf("BAND-01", "BAND-02", "BAND-03")
                }
            }
        }
    })
