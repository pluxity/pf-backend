package com.pluxity.safers.collect.config

import com.pluxity.safers.collect.service.CctvEventCollector
import com.pluxity.safers.global.config.KafkaProperties
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaTopicConfig(
    private val kafkaProperties: KafkaProperties,
) {
    @Bean
    fun kafkaAdmin(): KafkaAdmin = KafkaAdmin(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers))

    @Bean
    fun plxCctvEventsTopic(): NewTopic = NewTopic(CctvEventCollector.TOPIC_EVENTS, 1, 1.toShort())

    @Bean
    fun plxCctvEventVideosTopic(): NewTopic = NewTopic(CctvEventCollector.TOPIC_VIDEOS, 1, 1.toShort())

}
