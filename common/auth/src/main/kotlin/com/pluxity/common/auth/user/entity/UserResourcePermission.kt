package com.pluxity.common.auth.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_resource_permissions")
class UserResourcePermission(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "resource_type", nullable = false)
    val resourceType: String,
    @Column(name = "resource_id", nullable = false)
    val resourceId: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
