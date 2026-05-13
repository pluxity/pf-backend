package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.queue.ForwardQueue
import com.pluxity.safersCollect.v1.collect.adapter.BandTelemetryAdapter
import com.pluxity.safersCollect.v1.collect.dto.BandTelemetry
import io.github.springwolf.addons.generic_binding.annotation.AsyncGenericOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class BandTelemetryMqttHandler(
    private val objectMapper: ObjectMapper,
    private val adapter: BandTelemetryAdapter,
    private val queue: ForwardQueue,
) : MqttIngressHandler {
    override val topicSuffix: String = "band/telemetry"

    @AsyncListener(
        operation =
            AsyncOperation(
                channelName = "safers/collect/band/telemetry",
                description = "스마트밴드 단건 측정값. publisher = 스마트밴드.",
                payloadType = BandTelemetry::class,
            ),
    )
    @AsyncGenericOperationBinding(
        type = "mqtt",
        fields = ["qos=1", "retain=false"],
    )
    override fun handle(payload: ByteArray) {
        val request = objectMapper.readValue(payload, BandTelemetry::class.java)
        queue.enqueueAll(adapter.toEnvelopes(request))
    }
}
