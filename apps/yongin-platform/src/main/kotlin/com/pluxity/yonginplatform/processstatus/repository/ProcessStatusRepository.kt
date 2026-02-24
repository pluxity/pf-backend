package com.pluxity.yonginplatform.processstatus.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.yonginplatform.processstatus.entity.ProcessStatus
import com.pluxity.yonginplatform.processstatus.entity.WorkType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProcessStatusRepository :
    JpaRepository<ProcessStatus, Long>,
    KotlinJdslJpqlExecutor {
    @Query("SELECT p FROM ProcessStatus p WHERE p.workDate = (SELECT MAX(p2.workDate) FROM ProcessStatus p2)")
    fun findAllByLatestWorkDate(): List<ProcessStatus>

    fun existsByWorkType(workType: WorkType): Boolean
}
