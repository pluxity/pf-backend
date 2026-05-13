package com.pluxity.safersCollect.v1.collect.mqtt

import com.pluxity.safersCollect.queue.ForwardQueue
import com.pluxity.safersCollect.v1.collect.adapter.GasTelemetryAdapter
import com.pluxity.safersCollect.v1.collect.dto.GasTelemetry
import io.github.springwolf.addons.generic_binding.annotation.AsyncGenericOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class GasTelemetryMqttHandler(
    private val objectMapper: ObjectMapper,
    private val adapter: GasTelemetryAdapter,
    private val queue: ForwardQueue,
) : MqttIngressHandler {
    override val topicSuffix: String = "gas/telemetry"

    @AsyncListener(
        operation =
            AsyncOperation(
                channelName = "safers/collect/gas/telemetry",
                description = "가스 센서 단건 측정값. publisher = 가스 센서 디바이스.",
                payloadType = GasTelemetry::class,
            ),
    )
    @AsyncGenericOperationBinding(
        type = "mqtt",
        fields = ["qos=1", "retain=false"],
    )
    override fun handle(payload: ByteArray) {
        val request = objectMapper.readValue(payload, GasTelemetry::class.java)
        queue.enqueueAll(adapter.toEnvelopes(request))
    }
}
