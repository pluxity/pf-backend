package com.pluxity.yonginplatform.keymanagement.repository

import com.pluxity.yonginplatform.keymanagement.entity.KeyManagement
import com.pluxity.yonginplatform.keymanagement.entity.KeyManagementType
import org.springframework.data.jpa.repository.JpaRepository

interface KeyManagementRepository : JpaRepository<KeyManagement, Long> {
    fun existsByTypeAndDisplayOrder(
        type: KeyManagementType,
        displayOrder: Int,
    ): Boolean

    fun findBySelectedTrue(): List<KeyManagement>
}
