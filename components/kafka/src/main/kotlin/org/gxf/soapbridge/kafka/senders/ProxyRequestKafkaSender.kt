// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.kafka.senders

import mu.KotlinLogging
import org.gxf.soapbridge.kafka.properties.TopicsConfigurationProperties
import org.gxf.soapbridge.messaging.ProxyRequestsMessageSender
import org.gxf.soapbridge.messaging.messages.ProxyServerRequestMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ProxyRequestKafkaSender(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    topicConfiguration: TopicsConfigurationProperties
) : ProxyRequestsMessageSender {
    private val logger = KotlinLogging.logger {}

    private val topic = topicConfiguration.outgoing.requests

    override fun send(requestMessage: ProxyServerRequestMessage) {
        logger.debug("SOAP payload: ${requestMessage.soapPayload} to $topic")
        kafkaTemplate.send(topic, requestMessage.constructSignedString())
    }
}
