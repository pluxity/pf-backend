package com.pluxity.safersCollect.queue

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

@Component
class BackpressurePolicy(
    props: SafersCollectProperties,
    meterRegistry: MeterRegistry,
) {
    private val policy: DropPolicy = props.queue.dropPolicy
    private val droppedCounter: Counter =
        Counter
            .builder(METRIC_QUEUE_DROPPED)
            .description("ForwardQueue 가 가득 차서 폐기된 envelope 누적 수")
            .tag("policy", policy.name)
            .register(meterRegistry)

    fun onOverflow(
        queue: BlockingQueue<TelemetryEnvelope>,
        item: TelemetryEnvelope,
    ) {
        when (policy) {
            DropPolicy.DROP_OLDEST -> {
                while (!queue.offer(item)) {
                    if (queue.poll() != null) {
                        droppedCounter.increment()
                    }
                }
            }
        }
    }

    fun droppedTotal(): Long = droppedCounter.count().toLong()

    companion object {
        const val METRIC_QUEUE_DROPPED = "collect.queue.dropped"
    }
}
