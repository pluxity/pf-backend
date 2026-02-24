package com.pluxity.safers.configuration.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "configurations")
class Configuration(
    @Column(name = "config_key", nullable = false, unique = true)
    val key: String,
    @Column(name = "config_value", nullable = false)
    var value: String,
) : IdentityIdEntity()
