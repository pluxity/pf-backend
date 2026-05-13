package com.pluxity.safersCollect.config

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.pluxity.safersCollect.v1.collect.mqtt.MqttIngressHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
class MqttSubscriberStarter(
    private val client: Mqtt5AsyncClient,
    private val handlers: List<MqttIngressHandler>,
    private val props: SafersCollectProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        client
            .connect()
            .thenCompose {
                log.info { "MQTT connected to ${props.mqtt.host}:${props.mqtt.port} as ${props.mqtt.clientId}" }
                val subs =
                    handlers.map { handler ->
                        val topic = "${props.mqtt.topicPrefix}/${handler.topicSuffix}"
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
                            .whenComplete { _, ex ->
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
