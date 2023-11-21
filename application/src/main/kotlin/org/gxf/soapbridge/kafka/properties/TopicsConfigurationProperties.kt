// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.kafka.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kafka")
class TopicsConfigurationProperties(
    val outgoing: OutgoingTopicsConfiguration,
)

class OutgoingTopicsConfiguration(
    val requests: OutgoingTopic,
    val responses: OutgoingTopic
)

class OutgoingTopic(
    val topic: String
)