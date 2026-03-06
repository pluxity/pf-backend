package com.pluxity.common.auth.user.service

import com.pluxity.common.auth.user.entity.UserResourcePermission
import com.pluxity.common.auth.user.repository.UserResourcePermissionRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class UserResourcePermissionServiceKoTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val userResourcePermissionRepository: UserResourcePermissionRepository = mockk()
        val userResourcePermissionService =
            UserResourcePermissionService(
                userResourcePermissionRepository,
            )

        Given("리소스 권한 존재 여부를 확인할 때") {

            When("해당 권한이 존재하는 경우") {
                val userId = 1L
                val resourceType = "FACILITY"
                val resourceId = "100"

                every {
                    userResourcePermissionRepository.existsByUserIdAndResourceTypeAndResourceId(
                        userId,
                        resourceType,
                        resourceId,
                    )
                } returns true

                Then("true 반환") {
                    val result = userResourcePermissionService.exists(userId, resourceType, resourceId)
                    result shouldBe true
                }
            }

            When("해당 권한이 존재하지 않는 경우") {
                val userId = 1L
                val resourceType = "FACILITY"
                val resourceId = "999"

                every {
                    userResourcePermissionRepository.existsByUserIdAndResourceTypeAndResourceId(
                        userId,
                        resourceType,
                        resourceId,
                    )
                } returns false

                Then("false 반환") {
                    val result = userResourcePermissionService.exists(userId, resourceType, resourceId)
                    result shouldBe false
                }
            }
        }

        Given("리소스 권한 생성을 진행할 때") {

            When("해당 권한이 존재하지 않는 경우") {
                val userId = 1L
                val resourceType = "FACILITY"
                val resourceId = "100"

                every {
                    userResourcePermissionRepository.existsByUserIdAndResourceTypeAndResourceId(
                        userId,
                        resourceType,
                        resourceId,
                    )
                } returns false
                every {
                    userResourcePermissionRepository.save(any())
                } returns
                    UserResourcePermission(
                        userId = userId,
                        resourceType = resourceType,
                        resourceId = resourceId,
                    )

                Then("새로운 권한 생성") {
                    userResourcePermissionService.create(userId, resourceType, resourceId)
                    verify(exactly = 1) { userResourcePermissionRepository.save(any()) }
                }
            }

            When("해당 권한이 이미 존재하는 경우") {
                val userId = 1L
                val resourceType = "FACILITY"
                val resourceId = "100"

                every {
                    userResourcePermissionRepository.existsByUserIdAndResourceTypeAndResourceId(
                        userId,
                        resourceType,
                        resourceId,
                    )
                } returns true

                Then("저장 없이 종료") {
                    userResourcePermissionService.create(userId, resourceType, resourceId)
                    verify(exactly = 0) { userResourcePermissionRepository.save(any()) }
                }
            }
        }

        Given("리소스 권한 삭제를 진행할 때") {

            When("유효한 리소스 타입과 ID로 삭제 요청") {
                val resourceType = "FACILITY"
                val resourceId = "100"

                every {
                    userResourcePermissionRepository.deleteByResourceTypeAndResourceId(
                        resourceType,
                        resourceId,
                    )
                } just runs

                Then("정상 삭제") {
                    userResourcePermissionService.delete(resourceType, resourceId)
                    verify(exactly = 1) {
                        userResourcePermissionRepository.deleteByResourceTypeAndResourceId(
                            resourceType,
                            resourceId,
                        )
                    }
                }
            }
        }
    })
