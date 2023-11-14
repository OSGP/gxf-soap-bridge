// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.kafka.listeners

import io.micrometer.observation.annotation.Observed
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.gxf.soapbridge.messaging.messages.ProxyServerResponseMessage
import org.gxf.soapbridge.services.ProxyResponseHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class ProxyResponseKafkaListener(
    private val proxyResponseHandler: ProxyResponseHandler
) {
    private val logger = KotlinLogging.logger { }

    @Observed(name = "responses.consumed")
    @KafkaListener(topics = ["\${topics.incoming.responses}"], id = "gxf-response-consumer")
    @RetryableTopic(
        backoff = Backoff(value = 3000L),
        attempts = "2",
        include = [SocketTimeoutException::class]
    )
    fun consume(record: ConsumerRecord<String, String>) {
        logger.info("Received response")
        val responseMessage = ProxyServerResponseMessage.createInstanceFromString(record.value())
        proxyResponseHandler.handleIncomingResponse(responseMessage)
    }
}
