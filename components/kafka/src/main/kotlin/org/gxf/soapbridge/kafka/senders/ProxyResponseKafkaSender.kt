// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.kafka.senders

import mu.KotlinLogging
import org.gxf.soapbridge.kafka.properties.TopicsConfigurationProperties
import org.gxf.soapbridge.messaging.ProxyResponsesMessageSender
import org.gxf.soapbridge.messaging.messages.ProxyServerResponseMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ProxyResponseKafkaSender(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    topicConfiguration: TopicsConfigurationProperties
) : ProxyResponsesMessageSender {
    private val logger = KotlinLogging.logger {}

    private val topic = topicConfiguration.outgoing.responses

    override fun send(responseMessage: ProxyServerResponseMessage) {
        logger.debug("SOAP payload: ${responseMessage.soapResponse} to $topic")
        kafkaTemplate.send(topic, responseMessage.constructSignedString())
    }
}
