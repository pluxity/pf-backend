package com.pluxity.safersCollect.config

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnExpression($$"'${safety-collector.mqtt.host:}' != ''")
class MqttClientConfig {
    @Bean
    fun mqtt5AsyncClient(props: SafersCollectProperties): Mqtt5AsyncClient {
        val mqtt = checkNotNull(props.mqtt) { "safety-collector.mqtt 설정이 필요합니다." }
        return Mqtt5Client
            .builder()
            .identifier(mqtt.clientId)
            .serverHost(mqtt.host)
            .serverPort(mqtt.port)
            .automaticReconnectWithDefaultConfig()
            .buildAsync()
    }
}
