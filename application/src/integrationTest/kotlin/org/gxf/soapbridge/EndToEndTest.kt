// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.gxf.soapbridge.application.factories.SslContextFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.client.reactive.JdkClientHttpConnector
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.web.reactive.function.client.WebClient
import java.net.http.HttpClient
import java.time.Duration


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = ["requests", "responses"])
class EndToEndTest(
    @LocalServerPort private val soapPort: Int,
    @Autowired private val sslContextFactory: SslContextFactory,
) {
    private val proxyUrl = "https://localhost:$soapPort/proxy-server"
    private val methodPath = "/someSoapMethod"
    private val callUrl = "$proxyUrl$methodPath"

    @BeforeEach
    fun setUp() {
        wireMockExtension.stubFor(
            post(methodPath)
                .withRequestBody(equalToXml(soapBody))
                .willReturn(
                    ok().withBody(soapResponse)
                )
        )
    }

    @Test
    fun testRequestResponse() {
        // Arrange an SSL context for organisation "testClient" using its client certificate
        val sslContextForOrganisation = sslContextFactory.createSslContext("testClient")
        val httpClient = HttpClient.newBuilder()
            .sslContext(sslContextForOrganisation)
            .build()
        val webClient = WebClient.builder()
            .clientConnector(JdkClientHttpConnector(httpClient))
            .build()

        // Act: send SOAP request and get the answer
        val responseBody = webClient.post().uri(callUrl)
            .bodyValue(soapBody)
            .exchangeToMono { it.bodyToMono(String::class.java) }
            .timeout(Duration.ofSeconds(10))
            .block()

        // Assert
        assertThat(responseBody).isEqualTo(soapResponse)
    }

    val soapBody = """
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:m="http://www.example.org">
          <soap:Header>
          </soap:Header>
          <soap:Body>
            <m:GetStockPrice>
              <m:StockName>T</m:StockName>
            </m:GetStockPrice>
          </soap:Body>
        </soap:Envelope>
    """.trimIndent()

    val soapResponse = "Read This Fine Message"

    companion object {
        @JvmField
        @RegisterExtension
        val wireMockExtension: WireMockExtension =
            WireMockExtension.newInstance().options(
                wireMockConfig()
                    .httpDisabled(true).httpsPort(8888)
                    .keystorePath("src/integrationTest/resources/proxy.keystore.jks")
                    .keystorePassword("123456")
                    .keyManagerPassword("123456")
                    .keystoreType("PKCS12")
                    .trustStorePath("src/integrationTest/resources/proxy.truststore.jks")
                    .trustStorePassword("123456")
                    .trustStoreType("PKCS12")
                    .needClientAuth(true)
            ).build()
    }
}
