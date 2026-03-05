package com.pluxity.weekly.team.repository

import com.pluxity.common.auth.user.entity.User
import com.pluxity.weekly.team.entity.Team
import com.pluxity.weekly.team.entity.TeamMember
import org.springframework.data.jpa.repository.JpaRepository

interface TeamMemberRepository : JpaRepository<TeamMember, Long> {
    fun findByTeam(team: Team): List<TeamMember>

    fun existsByTeamAndUser(
        team: Team,
        user: User,
    ): Boolean

    fun deleteByTeamAndUser(
        team: Team,
        user: User,
    )
}
