package com.pluxity.safers.event.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
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
    @Column(name = "track_id", nullable = false)
    var trackId: Long,
    @Column(name = "name", nullable = false)
    var name: String,
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
    @Column(name = "site_id", nullable = false)
    var siteId: Long,
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
