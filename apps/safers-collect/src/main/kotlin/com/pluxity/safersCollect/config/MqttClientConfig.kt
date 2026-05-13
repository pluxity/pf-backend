package com.pluxity.safersCollect.config

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MqttClientConfig {
    @Bean
    fun mqtt5AsyncClient(props: SafersCollectProperties): Mqtt5AsyncClient =
        Mqtt5Client
            .builder()
            .identifier(props.mqtt.clientId)
            .serverHost(props.mqtt.host)
            .serverPort(props.mqtt.port)
            .automaticReconnectWithDefaultConfig()
            .buildAsync()
}
