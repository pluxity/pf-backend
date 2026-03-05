package com.pluxity.weekly.project.repository

import com.pluxity.weekly.project.entity.Project
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long>
