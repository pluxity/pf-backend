package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.v1.collect.adapter.GasEventAdapter
import com.pluxity.safersCollect.v1.collect.dto.GasEventRequest
import io.github.springwolf.addons.generic_binding.annotation.AsyncGenericOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class GasEventMqttHandler(
    private val objectMapper: ObjectMapper,
    private val adapter: GasEventAdapter,
    private val eventForwarder: EventForwarder,
) : MqttIngressHandler {
    override val topicSuffix: String = "gas/events"

    @AsyncListener(
        operation =
            AsyncOperation(
                channelName = "safers/collect/gas/events",
                description = "가스 임계 초과 이벤트. publisher = 가스 센서 디바이스. 임계 초과 시점에만 발행.",
                payloadType = GasEventRequest::class,
            ),
    )
    @AsyncGenericOperationBinding(
        type = "mqtt",
        fields = ["qos=1", "retain=false"],
    )
    override fun handle(payload: ByteArray) {
        val request = objectMapper.readValue(payload, GasEventRequest::class.java)
        eventForwarder.forward(adapter.toEnvelope(request))
    }
}
