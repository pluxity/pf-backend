package com.pluxity.safers.event.kafka

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.global.config.KafkaProperties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer

@EnableKafka
@Configuration
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties,
) {
    private fun baseProps(): Map<String, Any> =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "safers",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        )

    @Bean
    fun cctvEventListenerFactory(): ConcurrentKafkaListenerContainerFactory<String, EventCreateRequest> =
        ConcurrentKafkaListenerContainerFactory<String, EventCreateRequest>().apply {
            setConsumerFactory(
                DefaultKafkaConsumerFactory(
                    baseProps(),
                    StringDeserializer(),
                    JacksonJsonDeserializer(EventCreateRequest::class.java),
                ),
            )
        }

    @Bean
    fun cctvVideoListenerFactory(): ConcurrentKafkaListenerContainerFactory<String, CctvVideoMessage> =
        ConcurrentKafkaListenerContainerFactory<String, CctvVideoMessage>().apply {
            setConsumerFactory(
                DefaultKafkaConsumerFactory(
                    baseProps(),
                    StringDeserializer(),
                    JacksonJsonDeserializer(CctvVideoMessage::class.java),
                ),
            )
        }
}
