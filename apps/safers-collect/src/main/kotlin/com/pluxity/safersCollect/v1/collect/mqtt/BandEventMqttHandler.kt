package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.v1.collect.adapter.BandEventAdapter
import com.pluxity.safersCollect.v1.collect.dto.BandEventRequest
import io.github.springwolf.addons.generic_binding.annotation.AsyncGenericOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class BandEventMqttHandler(
    private val objectMapper: ObjectMapper,
    private val adapter: BandEventAdapter,
    private val eventForwarder: EventForwarder,
) : MqttIngressHandler {
    override val topicSuffix: String = "band/events"

    @AsyncListener(
        operation =
            AsyncOperation(
                channelName = "safers/collect/band/events",
                description = "스마트밴드 이상 이벤트 (VITAL_ABNORMAL / FALL_DETECTED / OFFLINE). eventType 으로 구분. publisher = 스마트밴드.",
                payloadType = BandEventRequest::class,
            ),
    )
    @AsyncGenericOperationBinding(
        type = "mqtt",
        fields = ["qos=1", "retain=false"],
    )
    override fun handle(payload: ByteArray) {
        val request = objectMapper.readValue(payload, BandEventRequest::class.java)
        eventForwarder.forward(adapter.toEnvelope(request))
    }
}
