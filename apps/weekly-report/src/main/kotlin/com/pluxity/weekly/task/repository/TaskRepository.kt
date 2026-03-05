package com.pluxity.weekly.task.repository

import com.pluxity.weekly.task.entity.Task
import org.springframework.data.jpa.repository.JpaRepository

interface TaskRepository : JpaRepository<Task, Long>
