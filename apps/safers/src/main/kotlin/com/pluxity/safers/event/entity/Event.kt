package com.pluxity.safers.event.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class Event(
    @Column(name = "event_id", nullable = false, unique = true)
    var eventId: String,
    @Column(name = "event_timestamp", nullable = false)
    var eventTimestamp: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    var category: EventCategory,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: EventType,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "site_id", nullable = false)
    var siteId: Long,
    @Column(name = "track_id")
    var trackId: Long? = null,
    @Column(name = "bbox")
    var bbox: String? = null,
    @Column(name = "center_x")
    var centerX: Double? = null,
    @Column(name = "center_y")
    var centerY: Double? = null,
    @Column(name = "confidence")
    var confidence: Double? = null,
    @Column(name = "path")
    var path: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 16)
    var severity: EventSeverity? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 16)
    var source: EventSource? = null,
    @Column(name = "device_id", length = 64)
    var deviceId: String? = null,
    @Column(name = "band_id", length = 64)
    var bandId: String? = null,
    @Column(name = "lat")
    var lat: Double? = null,
    @Column(name = "lon")
    var lon: Double? = null,
    @Column(name = "alt")
    var alt: Double? = null,
    @Column(name = "accuracy_m")
    var accuracyM: Double? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    var payload: Map<String, Any>? = null,
) : IdentityIdEntity() {
    @Column(name = "snapshot_file_id")
    var snapshotFileId: Long? = null
        private set

    @Column(name = "video_file_id")
    var videoFileId: Long? = null
        private set

    fun assignSnapshotFile(fileId: Long?) {
        snapshotFileId = fileId
    }

    fun assignVideoFile(fileId: Long?) {
        videoFileId = fileId
    }
}
