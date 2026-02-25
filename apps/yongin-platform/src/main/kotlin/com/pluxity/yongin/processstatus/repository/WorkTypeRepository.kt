package com.pluxity.yongin.processstatus.repository

import com.pluxity.yongin.processstatus.entity.WorkType
import org.springframework.data.jpa.repository.JpaRepository

interface WorkTypeRepository : JpaRepository<WorkType, Long>
