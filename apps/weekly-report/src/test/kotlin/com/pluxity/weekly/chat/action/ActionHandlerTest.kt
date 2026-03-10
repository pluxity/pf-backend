package com.pluxity.weekly.chat.action

import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.action.dto.ActionResultType
import com.pluxity.weekly.chat.dto.dummyClarifyAction
import com.pluxity.weekly.chat.dto.dummyDeleteAction
import com.pluxity.weekly.chat.dto.dummyReadAction
import com.pluxity.weekly.chat.dto.dummyUpsertAction
import com.pluxity.weekly.epic.entity.dummyEpic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.entity.dummyProject
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.dto.TaskResponse
import com.pluxity.weekly.task.entity.dummyTask
import com.pluxity.weekly.task.repository.TaskRepository
import com.pluxity.weekly.task.service.TaskService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class ActionHandlerTest :
    BehaviorSpec({

        val taskService: TaskService = mockk()
        val taskRepository: TaskRepository = mockk()
        val epicRepository: EpicRepository = mockk()
        val projectRepository: ProjectRepository = mockk()

        val handler =
            ActionHandler(
                taskService,
                taskRepository,
                epicRepository,
                projectRepository,
            )

        val userId = 3L

        Given("read 액션") {
            When("task 조회 요청이면") {
                val action = dummyReadAction(filters = mapOf("status" to "IN_PROGRESS"))
                val taskResponse: TaskResponse = mockk(relaxed = true)

                every { taskService.search(any()) } returns listOf(taskResponse)

                val result = handler.handle(action, userId)

                Then("SUCCESS 결과를 반환한다") {
                    result.type shouldBe ActionResultType.SUCCESS
                    result.message shouldBe "1개의 태스크를 조회했습니다."
                }
            }
        }

        Given("upsert 액션") {
            When("신규 task 생성 요청이면") {
                val action = dummyUpsertAction(name = "신규 태스크", epicId = 1L)

                every { taskService.create(any()) } returns 1L

                val result = handler.handle(action, userId)

                Then("SUCCESS 결과를 반환한다") {
                    result.type shouldBe ActionResultType.SUCCESS
                    result.message shouldBe "태스크 '신규 태스크'이(가) 생성되었습니다. (ID: 1)"
                }
            }

            When("기존 task 수정 요청이면") {
                val project = dummyProject(id = 1L, name = "SAFERS")
                val epic = dummyEpic(id = 1L, project = project, name = "인증")
                val task = dummyTask(id = 1L, epic = epic, name = "로그인 API")
                val action = dummyUpsertAction(id = 1L, name = "수정 태스크", epicId = 1L, status = "DONE", progress = 100)

                every { taskRepository.findByIdOrNull(1L) } returns task
                every { taskService.update(1L, any()) } just runs

                val result = handler.handle(action, userId)

                Then("SUCCESS 결과를 반환한다") {
                    result.type shouldBe ActionResultType.SUCCESS
                    result.message shouldBe "태스크 '수정 태스크'이(가) 수정되었습니다."
                }
            }

            When("이름으로 에픽을 해석하면") {
                val project = dummyProject(id = 1L, name = "SAFERS")
                val epic = dummyEpic(id = 5L, project = project, name = "인증 개발")
                val action = dummyUpsertAction(name = "로그인", epic = "인증")

                every { projectRepository.findByNameContainingIgnoreCase(any()) } returns emptyList()
                every { epicRepository.findByNameContainingIgnoreCase("인증") } returns listOf(epic)
                every { taskService.create(any()) } returns 2L

                val result = handler.handle(action, userId)

                Then("에픽 이름이 ID로 해석되어 생성된다") {
                    result.type shouldBe ActionResultType.SUCCESS
                    verify { taskService.create(match { it.epicId == 5L }) }
                }
            }

            When("에픽을 찾을 수 없으면") {
                val action = dummyUpsertAction(name = "태스크", epic = "존재하지않는에픽")

                every { projectRepository.findByNameContainingIgnoreCase(any()) } returns emptyList()
                every { epicRepository.findByNameContainingIgnoreCase("존재하지않는에픽") } returns emptyList()

                val result = handler.handle(action, userId)

                Then("ERROR 결과를 반환한다") {
                    result.type shouldBe ActionResultType.ERROR
                    result.message shouldBe "에픽을 찾을 수 없습니다."
                }
            }
        }

        Given("delete 액션") {
            When("task 삭제 요청이면") {
                val action = dummyDeleteAction(id = 1L)

                val result = handler.handle(action, userId)

                Then("NEEDS_CONFIRM 결과를 반환한다") {
                    result.type shouldBe ActionResultType.NEEDS_CONFIRM
                    result.message shouldBe "태스크(ID: 1)을(를) 삭제하시겠습니까?"

                    @Suppress("UNCHECKED_CAST")
                    val data = result.data as Map<String, Any>
                    data["taskId"] shouldBe 1L
                }
            }

            When("ID가 없으면") {
                val action = dummyDeleteAction().copy(id = null)

                val result = handler.handle(action, userId)

                Then("ERROR 결과를 반환한다") {
                    result.type shouldBe ActionResultType.ERROR
                    result.message shouldBe "삭제할 태스크의 ID가 필요합니다."
                }
            }
        }

        Given("clarify 액션") {
            When("clarify 요청이면") {
                val action = dummyClarifyAction()

                val result = handler.handle(action, userId)

                Then("CLARIFY 결과를 반환한다") {
                    result.type shouldBe ActionResultType.CLARIFY
                    result.message shouldBe "어느 프로젝트인가요?"
                    result.candidates shouldBe listOf("SAFERS", "용인 플랫폼")
                    result.partial shouldBe mapOf("action" to "delete")
                }
            }
        }

        Given("권한 에러 처리") {
            When("서비스 호출 시 권한 에러가 발생하면") {
                val action = dummyUpsertAction(name = "태스크", epicId = 1L)

                every { taskService.create(any()) } throws
                    CustomException(WeeklyReportErrorCode.NOT_FOUND_TASK, 999L)

                val result = handler.handle(action, userId)

                Then("ERROR 결과를 반환한다") {
                    result.type shouldBe ActionResultType.ERROR
                }
            }
        }

        Given("알 수 없는 액션") {
            When("지원하지 않는 액션이면") {
                val action = dummyReadAction().copy(action = "unknown")

                val result = handler.handle(action, userId)

                Then("ERROR 결과를 반환한다") {
                    result.type shouldBe ActionResultType.ERROR
                    result.message shouldBe "알 수 없는 액션: unknown"
                }
            }
        }
    })
