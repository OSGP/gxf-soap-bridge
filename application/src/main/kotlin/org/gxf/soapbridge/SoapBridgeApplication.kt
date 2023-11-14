// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication
@EnableWebMvc
@ConfigurationPropertiesScan
class SoapBridgeApplication

fun main(args: Array<String>) {
    runApplication<SoapBridgeApplication>(*args)
}
