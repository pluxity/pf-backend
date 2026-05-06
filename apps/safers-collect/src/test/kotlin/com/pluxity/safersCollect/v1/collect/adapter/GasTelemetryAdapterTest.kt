package com.pluxity.safersCollect.v1.collect.adapter

import com.pluxity.safersCollect.v1.collect.dto.GasCollectRequest
import com.pluxity.safersCollect.v1.collect.dto.GasMeasurement
import com.pluxity.safersCollect.v1.collect.dto.GasSample
import com.pluxity.safersCollect.v1.collect.enums.GasType
import com.pluxity.safersCollect.v1.collect.enums.GasUnit
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GasTelemetryAdapterTest :
    BehaviorSpec({

        val adapter = GasTelemetryAdapter()

        Given("한 sample 에 O2/H2S/CO/LEL 4개 측정값") {
            val request =
                GasCollectRequest(
                    samples =
                        listOf(
                            GasSample(
                                deviceId = "GAS-MH203-01",
                                measurements =
                                    listOf(
                                        GasMeasurement(
                                            gas = GasType.O2,
                                            value = 3.1,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.H2S,
                                            value = 1.2,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.CO,
                                            value = 1.5,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.LEL,
                                            value = 1.8,
                                            unit = GasUnit.PPM,
                                        ),
                                    ),
                                battery = 100,
                                signalRssi = -68,
                                timestamp = LocalDateTime.now(),
                            ),
                        ),
                )

            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("envelope 4개 생성, 각각 measurement=gas_reading") {
                    envelopes.size shouldBe 4
                    envelopes.map { it.measurement } shouldBe listOf("gas_reading", "gas_reading", "gas_reading", "gas_reading")
                }
                Then("각 envelope 의 tag.gas / tag.unit 이 측정값과 일치") {
                    envelopes.map { it.tags["gas"] } shouldBe listOf("O2", "H2S", "CO", "LEL")
                    envelopes.map { it.tags["unit"] } shouldBe listOf("PPM", "PPM", "PPM", "PPM")
                }
            }
        }

        Given("battery 와 signalRssi 가 모두 null 인 sample") {

            val request =
                GasCollectRequest(
                    samples =
                        listOf(
                            GasSample(
                                deviceId = "GAS-MH203-01",
                                measurements =
                                    listOf(
                                        GasMeasurement(
                                            gas = GasType.O2,
                                            value = 3.1,
                                            unit = GasUnit.PPM,
                                        ),
                                    ),
                                battery = null,
                                signalRssi = null,
                                timestamp = LocalDateTime.now(),
                            ),
                        ),
                )
            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("fields 에 value 만 있고 battery / signal_rssi 키는 없음") {
                    envelopes.map { it.fields.keys } shouldBe listOf(setOf("value"))
                }
            }
        }

        Given("타임스탬프 2026-04-24T10:15:30, deviceId GAS-MH203-01 sample") {
            val timestamp = LocalDateTime.of(2026, 4, 24, 10, 15, 30)

            val request =
                GasCollectRequest(
                    samples =
                        listOf(
                            GasSample(
                                deviceId = "GAS-MH203-01",
                                measurements =
                                    listOf(
                                        GasMeasurement(
                                            gas = GasType.O2,
                                            value = 3.1,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.H2S,
                                            value = 1.2,
                                            unit = GasUnit.PPM,
                                        ),
                                    ),
                                battery = 100,
                                signalRssi = -68,
                                timestamp = timestamp,
                            ),
                        ),
                )
            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("모든 envelope 의 timestamp 와 sourceId 가 동일") {
                    envelopes.forEach {
                        it.timestamp shouldBe timestamp
                        it.sourceId shouldBe "GAS-MH203-01"
                    }
                }
            }
        }

        Given("samples 2건 (각 3 가스, 2 가스)") {
            val request =
                GasCollectRequest(
                    samples =
                        listOf(
                            GasSample(
                                deviceId = "GAS-MH203-01",
                                measurements =
                                    listOf(
                                        GasMeasurement(
                                            gas = GasType.O2,
                                            value = 3.1,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.H2S,
                                            value = 1.2,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.CO,
                                            value = 1.5,
                                            unit = GasUnit.PPM,
                                        ),
                                    ),
                                battery = 100,
                                signalRssi = -68,
                                timestamp = LocalDateTime.now(),
                            ),
                            GasSample(
                                deviceId = "GAS-MH203-02",
                                measurements =
                                    listOf(
                                        GasMeasurement(
                                            gas = GasType.H2S,
                                            value = 1.2,
                                            unit = GasUnit.PPM,
                                        ),
                                        GasMeasurement(
                                            gas = GasType.CO,
                                            value = 1.5,
                                            unit = GasUnit.PPM,
                                        ),
                                    ),
                                battery = 100,
                                signalRssi = -68,
                                timestamp = LocalDateTime.now(),
                            ),
                        ),
                )
            When("toEnvelopes 호출") {
                val envelopes = adapter.toEnvelopes(request)
                Then("envelope 총 5개 (3+2) 가 List 로 평탄화되어 반환") {
                    envelopes.size shouldBe 5
                }
            }
        }
    })
