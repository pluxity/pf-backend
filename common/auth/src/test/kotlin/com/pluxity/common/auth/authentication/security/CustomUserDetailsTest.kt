package com.pluxity.common.auth.authentication.security

import com.pluxity.common.test.entity.dummyUser
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CustomUserDetailsTest :
    BehaviorSpec({
        Given("CustomUserDetails 객체가 주어졌을 때") {
            val user = dummyUser(password = "password123", username = "testUser")
            val userDetails = CustomUserDetails(user)

            When("password를 조회하면") {
                Then("사용자의 비밀번호를 반환한다") {
                    userDetails.password shouldBe "password123"
                }
            }

            When("username을 조회하면") {
                Then("사용자의 아이디를 반환한다") {
                    userDetails.username shouldBe "testUser"
                }
            }
        }
    })
