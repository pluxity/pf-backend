package com.pluxity.weekly.team.entity

import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import com.pluxity.common.test.entity.dummyUser

fun dummyTeam(
    id: Long? = null,
    name: String = "테스트 팀",
    leaderId: Long? = null,
) = Team(
    name = name,
    leaderId = leaderId,
).withId(id).withAudit()

fun dummyTeamMember(
    id: Long? = null,
    team: Team = dummyTeam(id = 1L),
    user: User = dummyUser(id = 1L),
) = TeamMember(
    team = team,
    user = user,
).withId(id).withAudit()
