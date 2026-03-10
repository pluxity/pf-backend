package com.pluxity.weekly.project.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.weekly.project.dto.ProjectMemberResponse
import com.pluxity.weekly.project.entity.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProjectRepository :
    JpaRepository<Project, Long>,
    KotlinJdslJpqlExecutor {
    fun findByNameContainingIgnoreCase(name: String): List<Project>

    @Query(
        """
        SELECT new com.pluxity.weekly.project.dto.ProjectMemberResponse(
            ea.epic.project.id, u.id, u.name, t.id, t.name
        )
        FROM EpicAssignment ea
        JOIN ea.assignedBy u
        LEFT JOIN TeamMember tm ON tm.user = u
        LEFT JOIN tm.team t
        WHERE ea.epic.project.id = :projectId
        GROUP BY ea.epic.project.id, u.id, u.name, t.id, t.name
        """,
    )
    fun findMembersByProjectId(projectId: Long): List<ProjectMemberResponse>

    @Query(
        """
        SELECT new com.pluxity.weekly.project.dto.ProjectMemberResponse(
            ea.epic.project.id, u.id, u.name, t.id, t.name
        )
        FROM EpicAssignment ea
        JOIN ea.assignedBy u
        LEFT JOIN TeamMember tm ON tm.user = u
        LEFT JOIN tm.team t
        WHERE ea.epic.project.id IN :projectIds
        GROUP BY ea.epic.project.id, u.id, u.name, t.id, t.name
        """,
    )
    fun findMembersByProjectIds(projectIds: List<Long>): List<ProjectMemberResponse>
}
