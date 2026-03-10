package com.pluxity.weekly.chat.context

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.test.entity.dummyUser
import com.pluxity.weekly.epic.entity.dummyEpic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.project.entity.dummyProject
import com.pluxity.weekly.task.entity.dummyTask
import com.pluxity.weekly.task.repository.TaskRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

class ContextBuilderTest :
    BehaviorSpec({

        val epicRepository: EpicRepository = mockk()
        val taskRepository: TaskRepository = mockk()
        val userRepository: UserRepository = mockk()
        val objectMapper: ObjectMapper = JsonMapper.builder().build()

        val builder =
            ContextBuilder(
                epicRepository,
                taskRepository,
                userRepository,
                objectMapper,
            )

        Given("컨텍스트 생성") {
            When("프로젝트, 에픽, 태스크가 존재하면") {
                val user = dummyUser(id = 1L, name = "홍길동")
                val project = dummyProject(id = 1L, name = "SAFERS")
                val epic = dummyEpic(id = 1L, project = project, name = "인증 개발")
                val task = dummyTask(id = 1L, epic = epic, name = "로그인 API")

                every { userRepository.findByIdOrNull(1L) } returns user
                every { epicRepository.findByAssignmentsAssignedById(1L) } returns listOf(epic)
                every { taskRepository.findByEpicInAndAssigneeId(listOf(epic), 1L) } returns listOf(task)

                val json = builder.build(1L)
                val context: Map<String, Any> = objectMapper.readValue(json)

                Then("today, user, projects 키가 포함된다") {
                    context shouldContainKey "today"
                    context shouldContainKey "user"
                    context shouldContainKey "projects"
                }

                Then("프로젝트 하위에 에픽과 태스크가 포함된다") {
                    @Suppress("UNCHECKED_CAST")
                    val projects = context["projects"] as List<Map<String, Any>>
                    projects.size shouldBe 1
                    projects[0]["name"] shouldBe "SAFERS"

                    @Suppress("UNCHECKED_CAST")
                    val epics = projects[0]["epics"] as List<Map<String, Any>>
                    epics.size shouldBe 1
                    epics[0]["name"] shouldBe "인증 개발"

                    @Suppress("UNCHECKED_CAST")
                    val tasks = epics[0]["tasks"] as List<Map<String, Any>>
                    tasks.size shouldBe 1
                    tasks[0]["name"] shouldBe "로그인 API"
                }
            }

            When("에픽에 태스크가 없으면") {
                val user = dummyUser(id = 2L, name = "김철수")
                val project = dummyProject(id = 2L, name = "WEEKLY")
                val epic = dummyEpic(id = 2L, project = project, name = "설계")

                every { userRepository.findByIdOrNull(2L) } returns user
                every { epicRepository.findByAssignmentsAssignedById(2L) } returns listOf(epic)
                every { taskRepository.findByEpicInAndAssigneeId(listOf(epic), 2L) } returns emptyList()

                val json = builder.build(2L)
                val context: Map<String, Any> = objectMapper.readValue(json)

                Then("에픽은 포함되고 태스크는 빈 리스트이다") {
                    @Suppress("UNCHECKED_CAST")
                    val projects = context["projects"] as List<Map<String, Any>>
                    projects.size shouldBe 1

                    @Suppress("UNCHECKED_CAST")
                    val epics = projects[0]["epics"] as List<Map<String, Any>>
                    epics.size shouldBe 1
                    epics[0]["name"] shouldBe "설계"

                    @Suppress("UNCHECKED_CAST")
                    val tasks = epics[0]["tasks"] as List<Map<String, Any>>
                    tasks.size shouldBe 0
                }
            }
        }
    })
