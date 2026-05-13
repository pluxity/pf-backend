package com.pluxity.safersCollect.config

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import com.pluxity.safersCollect.v1.collect.mqtt.MqttIngressHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
@ConditionalOnExpression($$"'${safety-collector.mqtt.host:}' != ''")
class MqttClientConfig {
    @Bean
    fun mqtt5AsyncClient(
        props: SafersCollectProperties,
        handlers: List<MqttIngressHandler>,
    ): Mqtt5AsyncClient {
        val handlersByTopic =
            handlers.associateBy { "${props.mqtt.topicPrefix}/${it.topicSuffix}" }

        lateinit var client: Mqtt5AsyncClient
        client =
            Mqtt5Client
                .builder()
                .identifier(props.mqtt.clientId)
                .serverHost(props.mqtt.host)
                .serverPort(props.mqtt.port)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener {
                    log.info { "MQTT connected to ${props.mqtt.host}:${props.mqtt.port} as ${props.mqtt.clientId}" }
                    handlersByTopic.keys.forEach { topic ->
                        client
                            .subscribeWith()
                            .topicFilter(topic)
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .send()
                            .whenComplete { _: Mqtt5SubAck?, ex: Throwable? ->
                                if (ex == null) {
                                    log.info { "MQTT subscribed: $topic (QoS 1)" }
                                } else {
                                    log.error(ex) { "MQTT subscribe failed: $topic" }
                                }
                            }
                    }
                }.buildAsync()

        client.publishes(MqttGlobalPublishFilter.SUBSCRIBED) { publish ->
            val topic = publish.topic.toString()
            val handler = handlersByTopic[topic]
            if (handler == null) {
                log.warn { "No handler registered for topic=$topic" }
                return@publishes
            }
            try {
                handler.handle(publish.payloadAsBytes)
            } catch (ex: Exception) {
                log.error(ex) { "MQTT handler failed for topic=$topic" }
            }
        }

        return client
    }
}
