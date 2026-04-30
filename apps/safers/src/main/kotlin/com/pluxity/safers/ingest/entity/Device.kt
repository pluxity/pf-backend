package com.pluxity.safers.ingest.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import com.pluxity.safers.ingest.enums.DeviceStatus
import com.pluxity.safers.ingest.enums.DeviceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "device",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_device_id_site", columnNames = ["device_id", "site_id"]),
    ],
)
class Device(
    @Column(name = "device_id", length = 64, nullable = false)
    var deviceId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    var type: DeviceType,
    @Column(name = "site_id", nullable = false)
    var siteId: Long,
    @Column(name = "name", length = 128)
    var name: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    var status: DeviceStatus = DeviceStatus.ACTIVE,
    @Column(name = "last_seen_at")
    var lastSeenAt: LocalDateTime? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    var metadata: Map<String, Any> = emptyMap(),
) : IdentityIdEntity()
