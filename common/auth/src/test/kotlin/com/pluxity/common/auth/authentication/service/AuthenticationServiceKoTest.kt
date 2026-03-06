package com.pluxity.common.auth.authentication.service

import com.pluxity.common.auth.authentication.repository.RefreshTokenRepository
import com.pluxity.common.auth.authentication.security.JwtProvider
import com.pluxity.common.auth.properties.JwtProperties
import com.pluxity.common.auth.properties.TokenProperties
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.test.dto.dummySignInRequest
import com.pluxity.common.test.dto.dummySignUpRequest
import com.pluxity.common.test.entity.dummyRefreshToken
import com.pluxity.common.test.entity.dummyUser
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder

class AuthenticationServiceKoTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val refreshTokenRepository: RefreshTokenRepository = mockk()
        val userRepository: UserRepository = mockk()
        val jwtProvider: JwtProvider = mockk()
        val authenticationManager: AuthenticationManager = mockk()
        val passwordEncoder: PasswordEncoder = mockk()
        val jwtProperties: JwtProperties = mockk(relaxed = true)

        every { jwtProperties.accessToken } returns TokenProperties("accessToken", "secret", 3600)
        every { jwtProperties.refreshToken } returns TokenProperties("refreshToken", "secret", 86400)

        val authenticationService =
            AuthenticationService(
                refreshTokenRepository,
                userRepository,
                jwtProvider,
                authenticationManager,
                passwordEncoder,
                jwtProperties,
            )

        Given("회원가입을 진행할 때") {

            When("유효한 요청으로 회원가입 요청") {
                val request = dummySignUpRequest()
                val user = dummyUser(username = request.username)

                every { userRepository.findByUsername(any()) } returns null
                every { passwordEncoder.encode(any()) } returns "encodedPassword"
                every { userRepository.save(any()) } returns user

                Then("사용자 ID 반환") {
                    val result = authenticationService.signUp(request)
                    result shouldBe user.requiredId
                    verify(exactly = 1) { userRepository.save(any()) }
                }
            }

            When("이미 존재하는 username으로 회원가입 요청") {
                val request = dummySignUpRequest()
                val existingUser = dummyUser(username = request.username)

                every { userRepository.findByUsername(any()) } returns existingUser

                Then("DUPLICATE_USERNAME 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        authenticationService.signUp(request)
                    }.code shouldBe ErrorCode.DUPLICATE_USERNAME
                }
            }
        }

        Given("로그인을 진행할 때") {

            When("유효한 요청으로 로그인 요청") {
                val signInRequest = dummySignInRequest()
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)
                val user = dummyUser(username = signInRequest.username)

                every { authenticationManager.authenticate(any()) } returns mockk()
                every { userRepository.findByUsername(any()) } returns user
                every { jwtProvider.generateAccessToken(any()) } returns "newAccessToken"
                every { jwtProvider.generateRefreshToken(any()) } returns "newRefreshToken"
                every { refreshTokenRepository.save(any()) } returns dummyRefreshToken()
                every { request.contextPath } returns ""

                Then("정상 로그인") {
                    authenticationService.signIn(signInRequest, request, response)
                    verify(exactly = 1) { authenticationManager.authenticate(any()) }
                    verify(exactly = 1) { jwtProvider.generateAccessToken(any()) }
                    verify(exactly = 1) { jwtProvider.generateRefreshToken(any()) }
                    verify(exactly = 1) { refreshTokenRepository.save(any()) }
                }
            }

            When("인증 실패로 로그인 요청") {
                val signInRequest = dummySignInRequest()
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)

                every { authenticationManager.authenticate(any()) } throws RuntimeException("인증 실패")

                Then("INVALID_ID_OR_PASSWORD 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        authenticationService.signIn(signInRequest, request, response)
                    }.code shouldBe ErrorCode.INVALID_ID_OR_PASSWORD
                }
            }

            When("인증은 성공하지만 사용자를 찾을 수 없는 경우") {
                val signInRequest = dummySignInRequest()
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)

                every { authenticationManager.authenticate(any()) } returns mockk()
                every { userRepository.findByUsername(any()) } returns null

                Then("NOT_FOUND_USER 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        authenticationService.signIn(signInRequest, request, response)
                    }.code shouldBe ErrorCode.NOT_FOUND_USER
                }
            }
        }

        Given("로그아웃을 진행할 때") {

            When("유효한 refreshToken이 있는 경우") {
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)
                val refreshToken = dummyRefreshToken()

                every { jwtProvider.getJwtFromRequest(any(), any()) } returns "tokenValue"
                every { refreshTokenRepository.findByToken(any()) } returns refreshToken
                every { refreshTokenRepository.delete(any()) } just runs
                every { request.contextPath } returns ""
                every { request.cookies } returns
                    arrayOf(
                        Cookie("accessToken", "access"),
                        Cookie("refreshToken", "refresh"),
                        Cookie("expiry", "12345"),
                    )

                Then("refreshToken 삭제 및 쿠키 클리어") {
                    authenticationService.signOut(request, response)
                    verify(exactly = 1) { refreshTokenRepository.delete(refreshToken) }
                }
            }

            When("refreshToken이 없는 경우") {
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)

                every { jwtProvider.getJwtFromRequest(any(), any()) } returns null

                Then("아무 동작 없이 종료") {
                    authenticationService.signOut(request, response)
                    verify(exactly = 0) { refreshTokenRepository.findByToken(any()) }
                    verify(exactly = 0) { refreshTokenRepository.delete(any()) }
                }
            }

            When("refreshToken은 있지만 Redis에 존재하지 않는 경우") {
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)

                every { jwtProvider.getJwtFromRequest(any(), any()) } returns "tokenValue"
                every { refreshTokenRepository.findByToken(any()) } returns null
                every { request.contextPath } returns ""
                every { request.cookies } returns
                    arrayOf(
                        Cookie("accessToken", "access"),
                        Cookie("refreshToken", "refresh"),
                        Cookie("expiry", "12345"),
                    )

                Then("삭제 없이 쿠키만 클리어") {
                    authenticationService.signOut(request, response)
                    verify(exactly = 0) { refreshTokenRepository.delete(any()) }
                }
            }
        }

        Given("토큰 갱신을 진행할 때") {

            When("유효한 refreshToken으로 갱신 요청") {
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)
                val user = dummyUser()

                every { jwtProvider.getJwtFromRequest(any(), any()) } returns "validRefreshToken"
                every { jwtProvider.validateRefreshToken(any()) } just runs
                every { jwtProvider.extractUsername(any(), any()) } returns user.username
                every { userRepository.findByUsername(any()) } returns user
                every { jwtProvider.generateAccessToken(any()) } returns "newAccessToken"
                every { jwtProvider.generateRefreshToken(any()) } returns "newRefreshToken"
                every { refreshTokenRepository.save(any()) } returns dummyRefreshToken()
                every { request.contextPath } returns ""

                Then("새로운 토큰 발급") {
                    authenticationService.refreshToken(request, response)
                    verify(exactly = 1) { jwtProvider.validateRefreshToken("validRefreshToken") }
                    verify(exactly = 1) { jwtProvider.extractUsername("validRefreshToken", true) }
                    verify(exactly = 1) { jwtProvider.generateAccessToken(user.username) }
                    verify(exactly = 1) { jwtProvider.generateRefreshToken(user.username) }
                    verify(exactly = 1) { refreshTokenRepository.save(any()) }
                }
            }

            When("refreshToken이 없는 경우") {
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)

                every { jwtProvider.getJwtFromRequest(any(), any()) } returns null

                Then("INVALID_REFRESH_TOKEN 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        authenticationService.refreshToken(request, response)
                    }.code shouldBe ErrorCode.INVALID_REFRESH_TOKEN
                }
            }

            When("refreshToken은 있지만 사용자를 찾을 수 없는 경우") {
                val request: HttpServletRequest = mockk(relaxed = true)
                val response: HttpServletResponse = mockk(relaxed = true)

                every { jwtProvider.getJwtFromRequest(any(), any()) } returns "validRefreshToken"
                every { jwtProvider.validateRefreshToken(any()) } just runs
                every { jwtProvider.extractUsername(any(), any()) } returns "unknownUser"
                every { userRepository.findByUsername(any()) } returns null

                Then("NOT_FOUND_USER 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        authenticationService.refreshToken(request, response)
                    }.code shouldBe ErrorCode.NOT_FOUND_USER
                }
            }
        }
    })
