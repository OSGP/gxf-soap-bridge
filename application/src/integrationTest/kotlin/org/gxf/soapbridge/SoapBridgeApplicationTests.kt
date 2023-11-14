// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka

@SpringBootTest
@EmbeddedKafka(topics = ["avroTopic"])
class SoapBridgeApplicationTests {

    @Test
    fun contextLoads() {
    }

}
