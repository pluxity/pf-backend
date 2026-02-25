package com.pluxity.yongin.cctv.repository

import com.pluxity.yongin.cctv.entity.CctvFavorite
import org.springframework.data.jpa.repository.JpaRepository

interface CctvFavoriteRepository : JpaRepository<CctvFavorite, Long> {
    fun findAllByOrderByDisplayOrderAsc(): List<CctvFavorite>

    fun existsByStreamName(streamName: String): Boolean
}
