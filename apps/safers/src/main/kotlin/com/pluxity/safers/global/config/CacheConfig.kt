package com.pluxity.safers.global.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager =
        RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(
                RedisCacheConfiguration
                    .defaultCacheConfig()
                    .disableCachingNullValues()
                    .entryTtl(Duration.ofHours(6))
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            GenericJacksonJsonRedisSerializer
                                .builder()
                                .enableDefaultTyping(
                                    BasicPolymorphicTypeValidator
                                        .builder()
                                        .allowIfSubType("com.pluxity.")
                                        .allowIfSubType("java.")
                                        .allowIfSubType("kotlin.")
                                        .build(),
                                ).build(),
                        ),
                    ),
            ).build()
}
