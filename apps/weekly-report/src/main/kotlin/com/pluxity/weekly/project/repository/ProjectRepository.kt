package com.pluxity.weekly.project.repository

import com.pluxity.weekly.project.dto.ProjectMemberResponse
import com.pluxity.weekly.project.entity.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProjectRepository : JpaRepository<Project, Long> {
    @Query(
        """
        SELECT new com.pluxity.weekly.project.dto.ProjectMemberResponse(
            u.id, u.name, t.id, t.name
        )
        FROM ProjectAssignment pa
        JOIN pa.assignedBy u
        LEFT JOIN TeamMember tm ON tm.user = u
        LEFT JOIN tm.team t
        WHERE pa.project.id = :projectId
        """,
    )
    fun findMembersByProjectId(projectId: Long): List<ProjectMemberResponse>
}
