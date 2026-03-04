package com.pluxity.weekly.team.repository

import com.pluxity.weekly.team.entity.TeamMember
import org.springframework.data.jpa.repository.JpaRepository

interface TeamMemberRepository : JpaRepository<TeamMember, Long> {
    fun findByTeamId(teamId: Long): List<TeamMember>

    fun existsByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    ): Boolean

    fun deleteByTeamIdAndUserId(
        teamId: Long,
        userId: Long,
    )
}
