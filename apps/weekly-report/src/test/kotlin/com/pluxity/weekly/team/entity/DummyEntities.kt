package com.pluxity.weekly.team.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId

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
    teamId: Long = 1L,
    userId: Long = 1L,
) = TeamMember(
    teamId = teamId,
    userId = userId,
).withId(id).withAudit()
