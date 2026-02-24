package com.pluxity.yonginplatform.goal.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "construction_section")
class ConstructionSection(
    @Column(name = "name", nullable = false, unique = true)
    val name: String,
) : IdentityIdEntity()
