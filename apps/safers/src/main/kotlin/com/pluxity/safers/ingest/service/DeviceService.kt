package com.pluxity.safers.ingest.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.ingest.dto.DeviceCreateRequest
import com.pluxity.safers.ingest.dto.DeviceResponse
import com.pluxity.safers.ingest.dto.DeviceStatus
import com.pluxity.safers.ingest.dto.DeviceType
import com.pluxity.safers.ingest.dto.DeviceUpdateRequest
import com.pluxity.safers.ingest.dto.toResponse
import com.pluxity.safers.ingest.entity.Device
import com.pluxity.safers.ingest.repository.DeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeviceService(
    private val deviceRepository: DeviceRepository,
) {
    @Transactional
    fun create(
        siteId: Long,
        request: DeviceCreateRequest,
    ): DeviceResponse {
        if (deviceRepository.existsByDeviceIdAndSiteId(request.id, siteId)) {
            throw CustomException(SafersErrorCode.DUPLICATE_DEVICE, "${request.id}@site=$siteId")
        }
        val device =
            Device(
                deviceId = request.id,
                type = request.type,
                siteId = siteId,
                name = request.name,
                metadata = request.metadata,
            )
        return deviceRepository.save(device).toResponse()
    }

    fun list(
        siteId: Long?,
        type: DeviceType?,
        status: DeviceStatus?,
    ): List<DeviceResponse> = deviceRepository.findAllByFilter(siteId, type, status).map { it.toResponse() }

    fun get(
        deviceId: String,
        siteId: Long,
    ): DeviceResponse = findOrThrow(deviceId, siteId).toResponse()

    @Transactional
    fun update(
        deviceId: String,
        siteId: Long,
        request: DeviceUpdateRequest,
    ): DeviceResponse {
        val device = findOrThrow(deviceId, siteId)
        request.name?.let { device.name = it }
        request.status?.let { device.status = it }
        request.metadata?.let { device.metadata = it }
        return device.toResponse()
    }

    @Transactional
    fun delete(
        deviceId: String,
        siteId: Long,
    ) {
        val device = findOrThrow(deviceId, siteId)
        deviceRepository.delete(device)
    }

    private fun findOrThrow(
        deviceId: String,
        siteId: Long,
    ): Device =
        deviceRepository.findByDeviceIdAndSiteId(deviceId, siteId)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_DEVICE, "$deviceId@site=$siteId")
}
