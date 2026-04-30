package com.pluxity.safers.ingest.repository

import com.pluxity.safers.ingest.entity.Device
import com.pluxity.safers.ingest.enums.DeviceStatus
import com.pluxity.safers.ingest.enums.DeviceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DeviceRepository : JpaRepository<Device, Long> {
    fun existsByDeviceIdAndSiteId(
        deviceId: String,
        siteId: Long,
    ): Boolean

    fun findByDeviceIdAndSiteId(
        deviceId: String,
        siteId: Long,
    ): Device?

    @Query(
        """
        SELECT d FROM Device d
        WHERE (:siteId IS NULL OR d.siteId = :siteId)
          AND (:type   IS NULL OR d.type   = :type)
          AND (:status IS NULL OR d.status = :status)
        """,
    )
    fun findAllByFilter(
        @Param("siteId") siteId: Long?,
        @Param("type") type: DeviceType?,
        @Param("status") status: DeviceStatus?,
    ): List<Device>
}
