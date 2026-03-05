package com.pluxity.weekly.epic.repository

import com.pluxity.weekly.epic.entity.Epic
import org.springframework.data.jpa.repository.JpaRepository

interface EpicRepository : JpaRepository<Epic, Long>
