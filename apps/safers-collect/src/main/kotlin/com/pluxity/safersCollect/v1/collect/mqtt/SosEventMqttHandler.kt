package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.forwarder.EventForwarder
import com.pluxity.safersCollect.v1.collect.adapter.SosEventAdapter
import com.pluxity.safersCollect.v1.collect.dto.SosEventRequest
import io.github.springwolf.addons.generic_binding.annotation.AsyncGenericOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class SosEventMqttHandler(
    private val objectMapper: ObjectMapper,
    private val adapter: SosEventAdapter,
    private val eventForwarder: EventForwarder,
) : MqttIngressHandler {
    override val topicSuffix: String = "sos/events"

    @AsyncListener(
        operation =
            AsyncOperation(
                channelName = "safers/collect/sos/events",
                description = "SOS 긴급호출 이벤트 (BUTTON_LONG_PRESS / MOBILE_APP / MANUAL_DISPATCH). publisher = 스마트밴드 / 모바일 앱 / 관제센터.",
                payloadType = SosEventRequest::class,
            ),
    )
    @AsyncGenericOperationBinding(
        type = "mqtt",
        fields = ["qos=1", "retain=false"],
    )
    override fun handle(payload: ByteArray) {
        val request = objectMapper.readValue(payload, SosEventRequest::class.java)
        eventForwarder.forward(adapter.toEnvelope(request))
    }
}
