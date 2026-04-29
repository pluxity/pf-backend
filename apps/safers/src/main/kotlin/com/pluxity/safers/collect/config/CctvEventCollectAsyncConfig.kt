package com.pluxity.safers.collect.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * CCTV 수집 리스너 전용 thread pool.
 * 기본 [taskExecutor] 와 분리해서 STOMP 브로드캐스트 등 다른 @Async 작업과 자원 격리.
 */
@Configuration
class CctvEventCollectAsyncConfig {
    @Bean(name = ["cctvCollectExecutor"])
    fun cctvCollectExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 16
            queueCapacity = 500
            setThreadNamePrefix("cctv-collect-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
        }
}
