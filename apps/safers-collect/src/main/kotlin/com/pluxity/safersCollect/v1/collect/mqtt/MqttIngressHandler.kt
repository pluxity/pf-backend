package com.pluxity.safersCollect.v1.collect.mqtt

interface MqttIngressHandler {
    val topicSuffix: String

    fun handle(payload: ByteArray)
}
