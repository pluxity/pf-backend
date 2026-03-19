package com.pluxity.weekly.epic.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.weekly.epic.entity.Epic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EpicRepository :
    JpaRepository<Epic, Long>,
    KotlinJdslJpqlExecutor {
    fun findByAssignmentsUserId(userId: Long): List<Epic>

    @Query("SELECT DISTINCT e FROM Epic e JOIN FETCH e.project JOIN e.assignments a WHERE a.user.id = :userId")
    fun findByAssignmentsUserIdWithProject(userId: Long): List<Epic>

    fun existsByAssignmentsUserIdAndId(
        userId: Long,
        epicId: Long,
    ): Boolean

    fun findByProjectIdIn(projectIds: List<Long>): List<Epic>
}
