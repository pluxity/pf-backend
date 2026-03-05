package com.pluxity.weekly.project.entity

import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import com.pluxity.common.test.entity.dummyUser
import java.time.LocalDate

fun dummyProjectAssignment(
    id: Long? = null,
    project: Project = dummyProject(id = 1L),
    assignedBy: User = dummyUser(id = 1L),
) = ProjectAssignment(
    project = project,
    assignedBy = assignedBy,
).withId(id).withAudit()

fun dummyProject(
    id: Long? = null,
    name: String = "테스트 프로젝트",
    description: String? = null,
    status: ProjectStatus = ProjectStatus.TODO,
    startDate: LocalDate? = null,
    dueDate: LocalDate? = null,
    pmId: Long? = null,
) = Project(
    name = name,
    description = description,
    status = status,
    startDate = startDate,
    dueDate = dueDate,
    pmId = pmId,
).withId(id).withAudit()
