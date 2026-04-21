package com.pluxity.safers.configuration.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.configuration.dto.dummyConfigurationRequest
import com.pluxity.safers.configuration.dto.dummyConfigurationUpdateRequest
import com.pluxity.safers.configuration.entity.Configuration
import com.pluxity.safers.configuration.entity.dummyConfiguration
import com.pluxity.safers.configuration.repository.ConfigurationRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ConfigurationServiceTest :
    BehaviorSpec({

        val configurationRepository: ConfigurationRepository = mockk(relaxed = true)
        val service = ConfigurationService(configurationRepository)

        Given("설정 값 조회 (findValue)") {

            When("존재하는 키를 조회하면") {
                val configuration = dummyConfiguration(key = "WEATHER_API", value = "key-1")
                every { configurationRepository.findByKey("WEATHER_API") } returns configuration

                val result = service.findValue("WEATHER_API")

                Then("값이 반환된다") {
                    result shouldBe "key-1"
                }
            }

            When("존재하지 않는 키를 조회하면") {
                every { configurationRepository.findByKey("UNKNOWN") } returns null

                val result = service.findValue("UNKNOWN")

                Then("null이 반환된다") {
                    result shouldBe null
                }
            }
        }

        Given("설정 생성") {

            When("중복되지 않은 키로 생성하면") {
                val request = dummyConfigurationRequest(key = "NEW_KEY", value = "v1")
                val saved = dummyConfiguration(id = 5L, key = "NEW_KEY", value = "v1")

                every { configurationRepository.findByKey("NEW_KEY") } returns null
                every { configurationRepository.save(any()) } returns saved

                val result = service.create(request)

                Then("설정 키가 반환된다") {
                    result shouldBe "NEW_KEY"
                }
            }

            When("이미 존재하는 키로 생성하면") {
                val request = dummyConfigurationRequest(key = "DUPLICATED", value = "v1")
                every { configurationRepository.findByKey("DUPLICATED") } returns dummyConfiguration(key = "DUPLICATED")

                Then("DUPLICATE_CONFIGURATION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.create(request)
                        }
                    exception.code shouldBe SafersErrorCode.DUPLICATE_CONFIGURATION
                }
            }
        }

        Given("설정 단건 조회 (findByKey)") {

            When("존재하는 키를 조회하면") {
                val configuration = dummyConfiguration(id = 1L, key = "WEATHER_API", value = "key-1")
                every { configurationRepository.findByKey("WEATHER_API") } returns configuration

                val result = service.findByKey("WEATHER_API")

                Then("설정 정보가 반환된다") {
                    result.key shouldBe "WEATHER_API"
                    result.value shouldBe "key-1"
                }
            }

            When("존재하지 않는 키를 조회하면") {
                every { configurationRepository.findByKey("UNKNOWN") } returns null

                Then("NOT_FOUND_CONFIGURATION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.findByKey("UNKNOWN")
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_CONFIGURATION
                }
            }
        }

        Given("설정 전체 조회 (findAll)") {

            When("목록을 조회하면") {
                val configurations =
                    listOf(
                        dummyConfiguration(id = 1L, key = "A", value = "a"),
                        dummyConfiguration(id = 2L, key = "B", value = "b"),
                    )
                every { configurationRepository.findAll(any<Pageable>()) } returns PageImpl(configurations)

                val result = service.findAll(PageSearchRequest(page = 1, size = 10))

                Then("페이징된 결과가 반환된다") {
                    result.content.size shouldBe 2
                    result.totalElements shouldBe 2
                    result.pageNumber shouldBe 1
                }
            }

            When("설정이 없으면") {
                every { configurationRepository.findAll(any<Pageable>()) } returns PageImpl(emptyList<Configuration>())

                val result = service.findAll(PageSearchRequest(page = 1, size = 10))

                Then("빈 결과가 반환된다") {
                    result.content.size shouldBe 0
                    result.totalElements shouldBe 0
                }
            }
        }

        Given("설정 수정") {

            When("존재하는 키의 값을 수정하면") {
                val configuration = dummyConfiguration(id = 1L, key = "WEATHER_API", value = "old")
                val request = dummyConfigurationUpdateRequest(value = "new")
                every { configurationRepository.findByKey("WEATHER_API") } returns configuration

                service.update("WEATHER_API", request)

                Then("값이 갱신된다") {
                    configuration.value shouldBe "new"
                }
            }

            When("존재하지 않는 키를 수정하면") {
                val request = dummyConfigurationUpdateRequest()
                every { configurationRepository.findByKey("UNKNOWN") } returns null

                Then("NOT_FOUND_CONFIGURATION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.update("UNKNOWN", request)
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_CONFIGURATION
                }
            }
        }

        Given("설정 삭제") {

            When("존재하는 키를 삭제하면") {
                val configuration = dummyConfiguration(id = 1L, key = "WEATHER_API")
                every { configurationRepository.findByKey("WEATHER_API") } returns configuration

                service.delete("WEATHER_API")

                Then("설정이 삭제된다") {
                    verify { configurationRepository.delete(configuration) }
                }
            }

            When("존재하지 않는 키를 삭제하면") {
                every { configurationRepository.findByKey("UNKNOWN") } returns null

                Then("NOT_FOUND_CONFIGURATION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            service.delete("UNKNOWN")
                        }
                    exception.code shouldBe SafersErrorCode.NOT_FOUND_CONFIGURATION
                }
            }
        }
    })
