package com.pluxity.weekly.team.service

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

    @Transactional
    fun create(request: TeamRequest): Long =
        teamRepository
            .save(
                Team(
                    name = request.name,
                    leaderId = request.leaderId,
                ),
            ).requiredId

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

    @Transactional
    fun delete(id: Long) {
        teamRepository.deleteById(getTeamById(id).requiredId)
    }

    // ── TeamMember ──

    fun findMembers(teamId: Long): List<TeamMemberResponse> {
        if(existsById(teamId))
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM, teamId)

        return memberRepository.findByTeamId(teamId).map { it.toResponse() }
    }

    @Transactional
    fun addMember(
        teamId: Long,
        userId: Long,
    ): Long {
        if(existsById(teamId))
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM, teamId)

        if (memberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw CustomException(WeeklyReportErrorCode.DUPLICATE_TEAM_MEMBER, userId, teamId)
        }
        return memberRepository.save(TeamMember(teamId = teamId, userId = userId)).requiredId
    }

    @Transactional
    fun removeMember(
        teamId: Long,
        userId: Long,
    ) {
        getTeamById(teamId)
        if (!memberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM_MEMBER, teamId, userId)
        }
        memberRepository.deleteByTeamIdAndUserId(teamId, userId)
    }

    private fun getTeamById(id: Long): Team =
        teamRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TEAM, id)

    private fun existsById(id: Long): Boolean =
        teamRepository.existsById(id)

}
