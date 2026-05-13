package com.pluxity.safersCollect.config

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnExpression($$"'${safety-collector.mqtt.host:}' != ''")
class MqttSubscriberStarter(
    private val client: Mqtt5AsyncClient,
    private val props: SafersCollectProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        log.info { "MQTT connecting to ${props.mqtt.host}:${props.mqtt.port} as ${props.mqtt.clientId}" }
        client.connect().exceptionally { ex ->
            log.error(ex) { "MQTT initial connect failed — automatic reconnect 가 백그라운드에서 재시도합니다" }
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
