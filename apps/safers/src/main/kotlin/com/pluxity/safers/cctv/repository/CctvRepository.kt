package com.pluxity.safers.cctv.repository

import com.pluxity.safers.cctv.entity.Cctv
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CctvRepository :
    JpaRepository<Cctv, Long>,
    CctvCustomRepository {
    fun findBySiteId(siteId: Long): List<Cctv>

    @Query("SELECT c FROM Cctv c JOIN FETCH c.site WHERE c.id = :id")
    fun findByIdWithSite(id: Long): Cctv?
}
