package com.pluxity.yonginplatform.cctv.repository

import com.pluxity.yonginplatform.cctv.entity.CctvFavorite
import org.springframework.data.jpa.repository.JpaRepository

interface CctvFavoriteRepository : JpaRepository<CctvFavorite, Long> {
    fun findAllByOrderByDisplayOrderAsc(): List<CctvFavorite>

    fun existsByStreamName(streamName: String): Boolean
}
