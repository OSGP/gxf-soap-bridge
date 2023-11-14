// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.kafka.listeners

import io.micrometer.observation.annotation.Observed
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.gxf.soapbridge.messaging.messages.ProxyServerRequestMessage
import org.gxf.soapbridge.services.ProxyRequestHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class ProxyRequestKafkaListener(private val proxyRequestHandler: ProxyRequestHandler) {
    private val logger = KotlinLogging.logger { }

    @Observed(name = "requests.consumed")
    @KafkaListener(topics = ["\${topics.incoming.requests}"], id = "gxf-request-consumer")
    @RetryableTopic(
        backoff = Backoff(value = 3000L),
        attempts = "2",
        include = [SocketTimeoutException::class]
    )
    fun consume(record: ConsumerRecord<String, String>) {
        logger.info("Received message")
        val requestMessage = ProxyServerRequestMessage.createInstanceFromString(record.value())
        proxyRequestHandler.handleIncomingRequest(requestMessage)
    }
}
