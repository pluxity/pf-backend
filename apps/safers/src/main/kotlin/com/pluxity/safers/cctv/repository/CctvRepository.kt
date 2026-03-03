package com.pluxity.safers.cctv.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.safers.cctv.entity.Cctv
import org.springframework.data.jpa.repository.JpaRepository

interface CctvRepository :
    JpaRepository<Cctv, Long>,
    KotlinJdslJpqlExecutor {
    fun findBySiteId(siteId: Long): List<Cctv>
}
