// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.kafka.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("topics")
class TopicsConfigurationProperties(
    val outgoing: RequestResponseTopics,
    val incoming: RequestResponseTopics
)

class RequestResponseTopics(
    val requests: String,
    val responses: String
)
