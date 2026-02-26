package com.pluxity.cctv.repository

import com.pluxity.cctv.entity.CctvBookmark
import org.springframework.data.jpa.repository.JpaRepository

interface CctvBookmarkRepository : JpaRepository<CctvBookmark, Long> {
    fun findAllByOrderByDisplayOrderAsc(): List<CctvBookmark>

    fun existsByStreamName(streamName: String): Boolean
}
