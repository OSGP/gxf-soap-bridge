// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.kafka.listeners

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.gxf.soapbridge.application.services.PlatformCommunicationService
import org.gxf.soapbridge.valueobjects.ProxyServerRequestMessage
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProxyRequestKafkaListener(private val platformCommunicationService: PlatformCommunicationService) {
    private val logger = KotlinLogging.logger { }

    @KafkaListener(topics = ["\${topics.incoming.requests}"], id = "gxf-request-consumer")
    fun consume(record: ConsumerRecord<String, String>) {
        logger.info("Received message")
        val requestMessage = ProxyServerRequestMessage.createInstanceFromString(record.value())
        platformCommunicationService.handleIncomingRequest(requestMessage)
    }
}
