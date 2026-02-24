package com.pluxity.yonginplatform.systemsetting.service

import com.pluxity.common.file.service.FileService
import com.pluxity.yonginplatform.systemsetting.dto.dummySystemSettingRequest
import com.pluxity.yonginplatform.systemsetting.entity.SystemSetting
import com.pluxity.yonginplatform.systemsetting.entity.dummySystemSetting
import com.pluxity.yonginplatform.systemsetting.repository.SystemSettingRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class SystemSettingServiceTest :
    BehaviorSpec({

        val repository: SystemSettingRepository = mockk(relaxed = true)
        val fileService: FileService = mockk(relaxed = true)
        val service = SystemSettingService(repository, fileService)

        Given("시스템 설정 조회") {

            When("저장된 설정이 있으면") {
                val entity = dummySystemSetting(rollingIntervalSeconds = 15)

                every { repository.findByIdOrNull(SystemSetting.SINGLETON_ID) } returns entity
                every { fileService.getFileResponse(any()) } returns null

                val result = service.find()

                Then("설정값이 반환된다") {
                    result.rollingIntervalSeconds shouldBe 15
                }
            }

            When("저장된 설정이 없으면") {
                every { repository.findByIdOrNull(SystemSetting.SINGLETON_ID) } returns null

                val result = service.find()

                Then("기본 응답이 반환된다") {
                    result.rollingIntervalSeconds shouldBe null
                }
            }
        }

        Given("시스템 설정 수정") {

            When("기존 설정이 있으면") {
                val existingEntity = dummySystemSetting(rollingIntervalSeconds = 10)
                val request = dummySystemSettingRequest(rollingIntervalSeconds = 30)

                every { repository.findByIdOrNull(SystemSetting.SINGLETON_ID) } returns existingEntity

                service.update(request)

                Then("기존 설정이 수정된다") {
                    existingEntity.rollingIntervalSeconds shouldBe 30
                    verify(exactly = 0) { repository.save(any()) }
                }
            }

            When("기존 설정이 없으면") {
                val request = dummySystemSettingRequest(rollingIntervalSeconds = 20)
                val savedEntity = dummySystemSetting(rollingIntervalSeconds = 20)

                every { repository.findByIdOrNull(SystemSetting.SINGLETON_ID) } returns null
                every { repository.save(any()) } returns savedEntity

                service.update(request)

                Then("새로운 설정이 저장된다") {
                    verify(exactly = 1) { repository.save(any()) }
                }
            }
        }
    })
