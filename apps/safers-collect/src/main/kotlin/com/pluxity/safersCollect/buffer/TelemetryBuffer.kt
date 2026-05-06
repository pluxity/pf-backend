package com.pluxity.safersCollect.buffer

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Component
class TelemetryBuffer(
    prop: SafersCollectProperties,
) {
    private val queue: BlockingQueue<TelemetryEnvelope> = LinkedBlockingQueue(prop.buffer.inMemoryMax)

    fun enqueueAll(items: Collection<TelemetryEnvelope>) {
        items.forEach {
            while (!queue.offer(it)) queue.poll()
        }
    }

    fun drain(maxItems: Int): List<TelemetryEnvelope> {
        val out = ArrayList<TelemetryEnvelope>(maxItems)
        queue.drainTo(out, maxItems)
        return out
    }

    fun size(): Int = queue.size
}
