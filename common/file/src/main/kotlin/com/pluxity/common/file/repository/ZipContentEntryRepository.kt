package com.pluxity.common.file.repository

import com.pluxity.common.file.entity.ZipContentEntry
import org.springframework.data.jpa.repository.JpaRepository

interface ZipContentEntryRepository : JpaRepository<ZipContentEntry, Long> {
    fun findByFileId(fileId: Long): List<ZipContentEntry>

    fun findByFileIdIn(fileIds: List<Long>): List<ZipContentEntry>
}
