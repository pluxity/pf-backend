package com.pluxity.safersCollect.queue

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Component
class ForwardQueue(
    props: SafersCollectProperties,
    private val backpressure: BackpressurePolicy,
) {
    private val queue: BlockingQueue<TelemetryEnvelope> = LinkedBlockingQueue(props.queue.normalCapacity)

    fun enqueueAll(items: Collection<TelemetryEnvelope>) {
        items.forEach { item ->
            if (!queue.offer(item)) {
                backpressure.onOverflow(queue, item)
            }
        }
    }

    fun drain(maxItems: Int): List<TelemetryEnvelope> {
        val out = ArrayList<TelemetryEnvelope>(maxItems)
        queue.drainTo(out, maxItems)
        return out
    }

    fun size(): Int = queue.size
}
