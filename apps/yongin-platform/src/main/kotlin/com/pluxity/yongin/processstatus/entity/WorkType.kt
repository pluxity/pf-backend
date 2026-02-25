package com.pluxity.yongin.processstatus.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "work_type")
class WorkType(
    @Column(name = "name", nullable = false, unique = true)
    val name: String,
) : IdentityIdEntity()
