package com.pluxity.weekly.project.service

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.test.entity.dummyUser
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.dto.dummyProjectRequest
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.project.entity.ProjectStatus
import com.pluxity.weekly.project.entity.dummyProject
import com.pluxity.weekly.project.entity.dummyProjectAssignment
import com.pluxity.weekly.project.repository.ProjectRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class ProjectServiceTest :
    BehaviorSpec({

        val projectRepository: ProjectRepository = mockk()
        val userRepository: UserRepository = mockk()
        val service = ProjectService(projectRepository, userRepository)

        Given("프로젝트 전체 조회") {
            When("프로젝트 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyProject(id = 1L, name = "프로젝트A"),
                        dummyProject(id = 2L, name = "프로젝트B"),
                        dummyProject(id = 3L, name = "프로젝트C"),
                    )

                every { projectRepository.findAll() } returns entities
                every { projectRepository.findMembersByProjectId(any()) } returns emptyList()

                val result = service.findAll()

                Then("전체 목록이 반환된다") {
                    result.size shouldBe 3
                    result[0].name shouldBe "프로젝트A"
                }
            }
        }

        Given("프로젝트 단건 조회") {
            When("존재하는 프로젝트를 조회하면") {
                val entity =
                    dummyProject(
                        id = 1L,
                        name = "테스트 프로젝트",
                        description = "설명",
                        status = ProjectStatus.IN_PROGRESS,
                        startDate = LocalDate.of(2026, 1, 1),
                        dueDate = LocalDate.of(2026, 3, 31),
                        pmId = 10L,
                    )

                every { projectRepository.findByIdOrNull(1L) } returns entity
                every { projectRepository.findMembersByProjectId(1L) } returns emptyList()

                val result = service.findById(1L)

                Then("프로젝트 정보가 반환된다") {
                    result.id shouldBe 1L
                    result.name shouldBe "테스트 프로젝트"
                    result.description shouldBe "설명"
                    result.status shouldBe ProjectStatus.IN_PROGRESS
                    result.startDate shouldBe LocalDate.of(2026, 1, 1)
                    result.dueDate shouldBe LocalDate.of(2026, 3, 31)
                    result.pmId shouldBe 10L
                }
            }

            When("존재하지 않는 프로젝트를 조회하면") {
                every { projectRepository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        service.findById(999L)
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_PROJECT
                }
            }
        }

        Given("프로젝트 생성") {
            When("유효한 요청으로 프로젝트를 생성하면") {
                val request =
                    dummyProjectRequest(
                        name = "신규 프로젝트",
                        status = ProjectStatus.TODO,
                        pmId = 5L,
                    )
                val saved = dummyProject(id = 1L, name = "신규 프로젝트", pmId = 5L)

                every { projectRepository.save(any<Project>()) } returns saved

                val result = service.create(request)

                Then("생성된 프로젝트의 ID가 반환된다") {
                    result shouldBe 1L
                }
            }
        }

        Given("프로젝트 수정") {
            When("존재하는 프로젝트를 수정하면") {
                val entity = dummyProject(id = 1L, name = "기존 프로젝트")
                val request =
                    dummyProjectRequest(
                        name = "수정된 프로젝트",
                        status = ProjectStatus.IN_PROGRESS,
                        pmId = 10L,
                    )

                every { projectRepository.findByIdOrNull(1L) } returns entity

                service.update(1L, request)

                Then("프로젝트 정보가 수정된다") {
                    entity.name shouldBe "수정된 프로젝트"
                    entity.status shouldBe ProjectStatus.IN_PROGRESS
                    entity.pmId shouldBe 10L
                }
            }

            When("존재하지 않는 프로젝트를 수정하면") {
                every { projectRepository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        service.update(999L, dummyProjectRequest())
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_PROJECT
                }
            }
        }

        Given("프로젝트 삭제") {
            When("존재하는 프로젝트를 삭제하면") {
                val entity = dummyProject(id = 1L, name = "삭제대상 프로젝트")

                every { projectRepository.findByIdOrNull(1L) } returns entity
                every { projectRepository.deleteById(1L) } just runs

                service.delete(1L)

                Then("삭제가 수행된다") {
                    verify(exactly = 1) { projectRepository.deleteById(1L) }
                }
            }

            When("존재하지 않는 프로젝트를 삭제하면") {
                every { projectRepository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        service.delete(999L)
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_PROJECT
                }
            }
        }

        // ── ProjectAssignment ──

        Given("프로젝트 배정 목록 조회") {
            When("프로젝트에 배정된 사용자를 조회하면") {
                val project = dummyProject(id = 1L)
                val user1 = dummyUser(id = 10L, name = "홍길동")
                val user2 = dummyUser(id = 20L, name = "김영희")
                project.assignments.addAll(
                    listOf(
                        dummyProjectAssignment(id = 1L, project = project, assignedBy = user1),
                        dummyProjectAssignment(id = 2L, project = project, assignedBy = user2),
                    ),
                )

                every { projectRepository.findByIdOrNull(1L) } returns project

                val result = service.findAssignments(1L)

                Then("배정 목록이 반환된다") {
                    result.size shouldBe 2
                }
            }
        }

        Given("프로젝트 배정 추가") {
            When("새로운 사용자를 프로젝트에 배정하면") {
                val project = dummyProject(id = 1L)
                val user = dummyUser(id = 10L)

                every { projectRepository.findByIdOrNull(1L) } returns project
                every { userRepository.findByIdOrNull(10L) } returns user

                service.assign(1L, 10L)

                Then("배정이 추가된다") {
                    project.assignments.size shouldBe 1
                    project.assignments[0].assignedBy shouldBe user
                }
            }

            When("이미 배정된 사용자를 추가하면") {
                val project = dummyProject(id = 1L)
                val user = dummyUser(id = 10L)
                project.assign(user)

                every { projectRepository.findByIdOrNull(1L) } returns project
                every { userRepository.findByIdOrNull(10L) } returns user

                val exception =
                    shouldThrow<CustomException> {
                        service.assign(1L, 10L)
                    }

                Then("DUPLICATE 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.DUPLICATE_PROJECT_ASSIGNMENT
                }
            }
        }

        Given("프로젝트 배정 해제") {
            When("배정된 사용자를 해제하면") {
                val project = dummyProject(id = 1L)
                val user = dummyUser(id = 10L)
                project.assign(user)

                every { projectRepository.findByIdOrNull(1L) } returns project
                every { userRepository.findByIdOrNull(10L) } returns user

                service.unassign(1L, 10L)

                Then("배정이 제거된다") {
                    project.assignments.size shouldBe 0
                }
            }

            When("배정되지 않은 사용자를 해제하면") {
                val project = dummyProject(id = 1L)
                val user = dummyUser(id = 999L)

                every { projectRepository.findByIdOrNull(1L) } returns project
                every { userRepository.findByIdOrNull(999L) } returns user

                val exception =
                    shouldThrow<CustomException> {
                        service.unassign(1L, 999L)
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_PROJECT_ASSIGNMENT
                }
            }
        }
    })
