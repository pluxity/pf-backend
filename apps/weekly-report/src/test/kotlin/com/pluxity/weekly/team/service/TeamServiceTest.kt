package com.pluxity.weekly.team.service

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.test.entity.dummyUser
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.team.dto.dummyTeamRequest
import com.pluxity.weekly.team.entity.Team
import com.pluxity.weekly.team.entity.TeamMember
import com.pluxity.weekly.team.entity.dummyTeam
import com.pluxity.weekly.team.entity.dummyTeamMember
import com.pluxity.weekly.team.repository.TeamMemberRepository
import com.pluxity.weekly.team.repository.TeamRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class TeamServiceTest :
    BehaviorSpec({

        val repository: TeamRepository = mockk()
        val memberRepository: TeamMemberRepository = mockk()
        val userRepository: UserRepository = mockk()
        val service = TeamService(repository, memberRepository, userRepository)

        Given("팀 전체 조회") {
            When("팀 목록을 조회하면") {
                val entities =
                    listOf(
                        dummyTeam(id = 1L, name = "개발팀"),
                        dummyTeam(id = 2L, name = "디자인팀"),
                        dummyTeam(id = 3L, name = "기획팀"),
                    )

                every { repository.findAll() } returns entities

                val result = service.findAll()

                Then("팀 목록이 반환된다") {
                    result.size shouldBe 3
                    result[0].name shouldBe "개발팀"
                    result[1].name shouldBe "디자인팀"
                    result[2].name shouldBe "기획팀"
                }
            }
        }

        Given("팀 단건 조회") {
            When("존재하는 팀을 조회하면") {
                val entity = dummyTeam(id = 1L, name = "개발팀", leaderId = 10L)

                every { repository.findByIdOrNull(1L) } returns entity

                val result = service.findById(1L)

                Then("팀 정보가 반환된다") {
                    result.id shouldBe 1L
                    result.name shouldBe "개발팀"
                    result.leaderId shouldBe 10L
                }
            }

            When("존재하지 않는 팀을 조회하면") {
                every { repository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        service.findById(999L)
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_TEAM
                }
            }
        }

        Given("팀 생성") {
            When("유효한 요청으로 팀을 생성하면") {
                val request = dummyTeamRequest(name = "신규팀", leaderId = 5L)
                val saved = dummyTeam(id = 1L, name = "신규팀", leaderId = 5L)

                every { repository.save(any<Team>()) } returns saved

                val result = service.create(request)

                Then("생성된 팀의 ID가 반환된다") {
                    result shouldBe 1L
                }
            }
        }

        Given("팀 수정") {
            When("존재하는 팀을 수정하면") {
                val entity = dummyTeam(id = 1L, name = "기존팀")
                val request = dummyTeamRequest(name = "수정팀", leaderId = 10L)

                every { repository.findByIdOrNull(1L) } returns entity

                service.update(1L, request)

                Then("팀 정보가 수정된다") {
                    entity.name shouldBe "수정팀"
                    entity.leaderId shouldBe 10L
                }
            }

            When("존재하지 않는 팀을 수정하면") {
                every { repository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        service.update(999L, dummyTeamRequest())
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_TEAM
                }
            }
        }

        Given("팀 삭제") {
            When("존재하는 팀을 삭제하면") {
                val entity = dummyTeam(id = 1L, name = "삭제대상팀")

                every { repository.findByIdOrNull(1L) } returns entity
                every { repository.deleteById(1L) } just runs

                service.delete(1L)

                Then("삭제가 수행된다") {
                    verify(exactly = 1) { repository.deleteById(1L) }
                }
            }

            When("존재하지 않는 팀을 삭제하면") {
                every { repository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        service.delete(999L)
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_TEAM
                }
            }
        }

        // ── TeamMember ──

        Given("팀원 목록 조회") {
            When("팀에 소속된 팀원을 조회하면") {
                val team = dummyTeam(id = 1L)
                val user1 = dummyUser(id = 10L, name = "홍길동")
                val user2 = dummyUser(id = 20L, name = "김영희")
                val members =
                    listOf(
                        dummyTeamMember(id = 1L, team = team, user = user1),
                        dummyTeamMember(id = 2L, team = team, user = user2),
                    )

                every { repository.findByIdOrNull(1L) } returns team
                every { memberRepository.findByTeam(team) } returns members

                val result = service.findMembers(1L)

                Then("팀원 목록이 반환된다") {
                    result.size shouldBe 2
                    result[0].id shouldBe 10L
                    result[1].id shouldBe 20L
                }
            }
        }

        Given("팀원 추가") {
            When("새로운 사용자를 팀에 추가하면") {
                val team = dummyTeam(id = 1L)
                val user = dummyUser(id = 10L)
                val saved = dummyTeamMember(id = 1L, team = team, user = user)

                every { repository.findByIdOrNull(1L) } returns team
                every { userRepository.findByIdOrNull(10L) } returns user
                every { memberRepository.existsByTeamAndUser(team, user) } returns false
                every { memberRepository.save(any<TeamMember>()) } returns saved

                val result = service.addMember(1L, 10L)

                Then("팀원 ID가 반환된다") {
                    result shouldBe 1L
                }
            }

            When("이미 소속된 사용자를 추가하면") {
                val team = dummyTeam(id = 1L)
                val user = dummyUser(id = 10L)

                every { repository.findByIdOrNull(1L) } returns team
                every { userRepository.findByIdOrNull(10L) } returns user
                every { memberRepository.existsByTeamAndUser(team, user) } returns true

                val exception =
                    shouldThrow<CustomException> {
                        service.addMember(1L, 10L)
                    }

                Then("DUPLICATE 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.DUPLICATE_TEAM_MEMBER
                }
            }
        }

        Given("팀원 제거") {
            When("소속된 사용자를 제거하면") {
                val team = dummyTeam(id = 1L)
                val user = dummyUser(id = 10L)

                every { repository.findByIdOrNull(1L) } returns team
                every { userRepository.findByIdOrNull(10L) } returns user
                every { memberRepository.existsByTeamAndUser(team, user) } returns true
                every { memberRepository.deleteByTeamAndUser(team, user) } just runs

                service.removeMember(1L, 10L)

                Then("삭제가 수행된다") {
                    verify(exactly = 1) { memberRepository.deleteByTeamAndUser(team, user) }
                }
            }

            When("소속되지 않은 사용자를 제거하면") {
                val team = dummyTeam(id = 1L)
                val user = dummyUser(id = 999L)

                every { repository.findByIdOrNull(1L) } returns team
                every { userRepository.findByIdOrNull(999L) } returns user
                every { memberRepository.existsByTeamAndUser(team, user) } returns false

                val exception =
                    shouldThrow<CustomException> {
                        service.removeMember(1L, 999L)
                    }

                Then("NOT_FOUND 예외가 발생한다") {
                    exception.code shouldBe WeeklyReportErrorCode.NOT_FOUND_TEAM_MEMBER
                }
            }
        }
    })
