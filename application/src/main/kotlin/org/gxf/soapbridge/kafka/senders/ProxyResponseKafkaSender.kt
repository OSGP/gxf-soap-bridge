// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.kafka.senders

import mu.KotlinLogging
import org.gxf.soapbridge.kafka.properties.TopicsConfigurationProperties
import org.gxf.soapbridge.valueobjects.ProxyServerResponseMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ProxyResponseKafkaSender(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    topicConfiguration: TopicsConfigurationProperties
) {
    private val logger = KotlinLogging.logger {}

    private val topic = topicConfiguration.outgoing.responses

    fun send(responseMessage: ProxyServerResponseMessage) {
        logger.debug { "SOAP payload: ${responseMessage.soapResponse} to $topic" }
        kafkaTemplate.send(topic, responseMessage.constructSignedString())
    }
}
