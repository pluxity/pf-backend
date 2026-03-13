package com.pluxity.weekly.epic.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.weekly.epic.entity.Epic
import org.springframework.data.jpa.repository.JpaRepository

interface EpicRepository :
    JpaRepository<Epic, Long>,
    KotlinJdslJpqlExecutor {
    fun findByNameContainingIgnoreCase(name: String): List<Epic>

    fun findByNameContainingIgnoreCaseAndProjectId(
        name: String,
        projectId: Long,
    ): List<Epic>

    fun findByAssignmentsAssignedById(userId: Long): List<Epic>

    fun findByProjectIdIn(projectIds: List<Long>): List<Epic>
}
