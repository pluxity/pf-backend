package com.pluxity.safers.event.kafka

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.global.config.KafkaProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.util.backoff.FixedBackOff

private val logger = KotlinLogging.logger {}

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
    fun kafkaErrorHandler(): DefaultErrorHandler =
        DefaultErrorHandler({ record, ex ->
            logger.error(ex) { "Kafka 소비 최종 실패 (DLT 전송 생략): topic=${record.topic()}, key=${record.key()}" }
        }, FixedBackOff(1000L, 3L)).apply {
            addRetryableExceptions(RetryableException::class.java)
            setRetryListeners({ record, ex, deliveryAttempt ->
                logger.warn(ex) { "Kafka 소비 재시도 (${deliveryAttempt}회): topic=${record.topic()}, key=${record.key()}" }
            })
        }

    @Bean
    fun cctvEventListenerFactory(
        kafkaErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, EventCreateRequest> =
        ConcurrentKafkaListenerContainerFactory<String, EventCreateRequest>().apply {
            setConsumerFactory(
                DefaultKafkaConsumerFactory(
                    baseProps(),
                    StringDeserializer(),
                    JacksonJsonDeserializer(EventCreateRequest::class.java),
                ),
            )
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean
    fun cctvVideoListenerFactory(
        kafkaErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, CctvVideoMessage> =
        ConcurrentKafkaListenerContainerFactory<String, CctvVideoMessage>().apply {
            setConsumerFactory(
                DefaultKafkaConsumerFactory(
                    baseProps(),
                    StringDeserializer(),
                    JacksonJsonDeserializer(CctvVideoMessage::class.java),
                ),
            )
            setCommonErrorHandler(kafkaErrorHandler)
        }
}
