// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.configuration.kafka

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfiguration {

    /** Retry messages two times before giving up on the message */
    @Bean
    fun errorHandler(): DefaultErrorHandler {
        return DefaultErrorHandler(FixedBackOff(0, 2L))
    }
}
