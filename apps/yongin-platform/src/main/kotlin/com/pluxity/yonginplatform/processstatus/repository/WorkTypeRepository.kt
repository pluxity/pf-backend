package com.pluxity.yonginplatform.processstatus.repository

import com.pluxity.yonginplatform.processstatus.entity.WorkType
import org.springframework.data.jpa.repository.JpaRepository

interface WorkTypeRepository : JpaRepository<WorkType, Long>
