package com.pluxity.weekly.team.service

import com.pluxity.common.auth.annotation.CheckPermission
import com.pluxity.common.auth.user.entity.PermissionAction
import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.team.dto.TeamMemberResponse
import com.pluxity.weekly.team.dto.TeamRequest
import com.pluxity.weekly.team.dto.TeamResponse
import com.pluxity.weekly.team.dto.toResponse
import com.pluxity.weekly.team.entity.Team
import com.pluxity.weekly.team.entity.TeamMember
import com.pluxity.weekly.team.repository.TeamMemberRepository
import com.pluxity.weekly.team.repository.TeamRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TeamService(
    private val teamRepository: TeamRepository,
    private val memberRepository: TeamMemberRepository,
    private val userRepository: UserRepository,
) {
    fun findAll(request: PageSearchRequest): PageResponse<TeamResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)
        val page =
            teamRepository.findPageNotNull(pageable) {
                select(entity(Team::class))
                    .from(entity(Team::class))
                    .orderBy(path(Team::id).desc())
            }
        return page.toPageResponse { it.toResponse() }
    }

    fun findById(id: Long): TeamResponse = getTeamById(id).toResponse()

    @CheckPermission(action = PermissionAction.CREATE, resourceType = "team")
    @Transactional
    fun create(request: TeamRequest): Long =
        teamRepository
            .save(
                Team(
                    name = request.name,
                    leaderId = request.leaderId,
                ),
            ).requiredId

    @CheckPermission(action = PermissionAction.UPDATE, resourceType = "team")
    @Transactional
    fun update(
        id: Long,
        request: TeamRequest,
    ) {
        getTeamById(id).update(
            name = request.name,
            leaderId = request.leaderId,
        )
    }

    @CheckPermission(action = PermissionAction.DELETE, resourceType = "team")
    @Transactional
    fun delete(id: Long) {
        teamRepository.deleteById(getTeamById(id).requiredId)
    }

    // ── TeamMember ──

    fun findMembers(teamId: Long): List<TeamMemberResponse> {
        val team = getTeamById(teamId)
        return memberRepository.findByTeam(team).map { it.toResponse() }
    }

    @CheckPermission(action = PermissionAction.CREATE, resourceType = "team")
    @Transactional
    fun addMember(
        teamId: Long,
        userId: Long,
    ): Long {
        val team = getTeamById(teamId)
        val user = getUserById(userId)
        if (memberRepository.existsByTeamAndUser(team, user)) {
            throw CustomException(WeeklyReportErrorCode.DUPLICATE_TEAM_MEMBER, userId, teamId)
        }
        return memberRepository.save(TeamMember(team = team, user = user)).requiredId
    }

    @CheckPermission(action = PermissionAction.DELETE, resourceType = "team")
    @Transactional
    fun removeMember(
        teamId: Long,
        userId: Long,
    ) {
        val team = getTeamById(teamId)
        val user = getUserById(userId)
        if (!memberRepository.existsByTeamAndUser(team, user)) {
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM_MEMBER, teamId, userId)
        }
        memberRepository.deleteByTeamAndUser(team, user)
    }

    private fun getTeamById(id: Long): Team =
        teamRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM, id)

    private fun getUserById(id: Long): User =
        userRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, id)
}
