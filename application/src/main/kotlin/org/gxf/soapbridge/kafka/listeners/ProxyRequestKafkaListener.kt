// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.kafka.listeners

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.gxf.soapbridge.application.services.PlatformCommunicationService
import org.gxf.soapbridge.valueobjects.ProxyServerRequestMessage
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProxyRequestKafkaListener(private val platformCommunicationService: PlatformCommunicationService) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        id = "gxf-request-consumer",
        topics = ["\${kafka.incoming.requests.topic}"],
        concurrency = "\${kafka.incoming.requests.concurrency}",
        idIsGroup = false
    )
    fun consume(record: ConsumerRecord<String, String>) {
        logger.debug { "Received request: ${record.key()}, ${record.value()}" }
        val requestMessage = ProxyServerRequestMessage.createInstanceFromString(record.value())
        platformCommunicationService.handleIncomingRequest(requestMessage)
    }
}
