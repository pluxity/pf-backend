package com.pluxity.safersCollect.config

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import com.pluxity.safersCollect.v1.collect.mqtt.MqttIngressHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnExpression($$"'${safety-collector.mqtt.host:}' != ''")
class MqttSubscriberStarter(
    private val client: Mqtt5AsyncClient,
    private val handlers: List<MqttIngressHandler>,
    private val props: SafersCollectProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        val mqtt = checkNotNull(props.mqtt) { "safety-collector.mqtt 설정이 필요합니다." }
        client
            .connect()
            .thenCompose {
                log.info { "MQTT connected to ${mqtt.host}:${mqtt.port} as ${mqtt.clientId}" }
                val subs =
                    handlers.map { handler ->
                        val topic = "${mqtt.topicPrefix}/${handler.topicSuffix}"
                        client
                            .subscribeWith()
                            .topicFilter(topic)
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .callback { publish ->
                                try {
                                    handler.handle(publish.payloadAsBytes)
                                } catch (ex: Exception) {
                                    log.error(ex) { "MQTT handler failed for topic=$topic" }
                                }
                            }.send()
                            .whenComplete { _: Mqtt5SubAck?, ex: Throwable? ->
                                if (ex == null) {
                                    log.info { "MQTT subscribed: $topic (QoS 1)" }
                                } else {
                                    log.error(ex) { "MQTT subscribe failed: $topic" }
                                }
                            }
                    }
                CompletableFuture.allOf(*subs.toTypedArray())
            }.exceptionally { ex ->
                log.error(ex) { "MQTT startup failed" }
                null
            }
    }

    @PreDestroy
    fun stop() {
        try {
            client.disconnect().get(5, TimeUnit.SECONDS)
            log.info { "MQTT disconnected" }
        } catch (ex: Exception) {
            log.warn(ex) { "MQTT disconnect did not complete cleanly" }
        }
    }
}
