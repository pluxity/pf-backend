package com.pluxity.weekly.epic.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.project.entity.dummyProject
import com.pluxity.weekly.team.entity.Team
import java.time.LocalDate

fun dummyEpic(
    id: Long? = null,
    project: Project = dummyProject(id = 1L),
    name: String = "테스트 에픽",
    description: String? = null,
    status: EpicStatus = EpicStatus.TODO,
    startDate: LocalDate? = null,
    dueDate: LocalDate? = null,
    team: Team? = null,
) = Epic(
    project = project,
    name = name,
    description = description,
    status = status,
    startDate = startDate,
    dueDate = dueDate,
    team = team,
).withId(id).withAudit()
