// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.kafka.listeners

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.gxf.soapbridge.application.services.ClientCommunicationService
import org.gxf.soapbridge.valueobjects.ProxyServerResponseMessage
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProxyResponseKafkaListener(
    private val clientCommunicationService: ClientCommunicationService
) {
    private val logger = KotlinLogging.logger { }

    @KafkaListener(topics = ["\${topics.incoming.responses}"], id = "gxf-response-consumer")
    fun consume(record: ConsumerRecord<String, String>) {
        logger.info("Received response")
        val responseMessage = ProxyServerResponseMessage.createInstanceFromString(record.value())
        clientCommunicationService.handleIncomingResponse(responseMessage)
    }
}
