package com.pluxity.safers.collect.config

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.global.config.KafkaProperties
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
class KafkaProducerConfig(
    private val kafkaProperties: KafkaProperties,
) {
    private fun producerProps(): Map<String, Any> =
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java,
            JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS to false,
        )

    @Bean
    fun eventKafkaTemplate(): KafkaTemplate<String, EventCreateRequest> = KafkaTemplate(DefaultKafkaProducerFactory(producerProps()))

    @Bean
    fun videoKafkaTemplate(): KafkaTemplate<String, CctvVideoMessage> = KafkaTemplate(DefaultKafkaProducerFactory(producerProps()))
}
