package com.pluxity.weekly.task.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.task.entity.Task
import org.springframework.data.jpa.repository.JpaRepository

interface TaskRepository :
    JpaRepository<Task, Long>,
    KotlinJdslJpqlExecutor {

    fun findByEpicInAndAssigneeId(
        epics: List<Epic>,
        assigneeId: Long,
    ): List<Task>

    fun existsByEpicIdAndName(
        epicId: Long,
        name: String,
    ): Boolean
}
