package com.pluxity.yongin.global.controller

import com.ninjasquad.springmockk.MockkBean
import com.pluxity.common.auth.user.service.UserService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(UserNameController::class)
class UserNameControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val userService: UserService,
) : BehaviorSpec({

        Given("사용자 이름 목록 조회 API") {

            When("GET /users/usernames - 성공") {
                every { userService.findAllUserNames() } returns listOf("user1", "user2", "user3")

                val result =
                    mockMvc.get("/users/usernames") {
                        with(user("tester"))
                    }

                Then("200 OK와 사용자 이름 목록이 반환된다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.data") { isArray() }
                        jsonPath("$.data.length()") { value(3) }
                        jsonPath("$.data[0]") { value("user1") }
                    }
                }
            }
        }
    })
