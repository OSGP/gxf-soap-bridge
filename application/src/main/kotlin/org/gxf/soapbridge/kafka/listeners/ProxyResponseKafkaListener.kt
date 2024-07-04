// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.kafka.listeners

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.gxf.soapbridge.application.services.ClientCommunicationService
import org.gxf.soapbridge.valueobjects.ProxyServerResponseMessage
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProxyResponseKafkaListener(private val clientCommunicationService: ClientCommunicationService) {
    private val logger = KotlinLogging.logger { }

    @KafkaListener(
        id = "gxf-response-consumer",
        topics = ["\${kafka.incoming.responses.topic}"],
        concurrency = "\${kafka.incoming.responses.concurrency}",
        idIsGroup = false
    )
    fun consume(record: ConsumerRecord<String, String>) {
        logger.debug { "Received response: ${record.key()}, ${record.value()}" }
        val responseMessage = ProxyServerResponseMessage.createInstanceFromString(record.value())
        clientCommunicationService.handleIncomingResponse(responseMessage)
    }
}
