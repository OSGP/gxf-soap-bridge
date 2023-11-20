// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.clients;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;
import org.gxf.soapbridge.application.factories.HttpsUrlConnectionFactory;
import org.gxf.soapbridge.application.services.SigningService;
import org.gxf.soapbridge.configuration.properties.HostnameVerificationStrategy;
import org.gxf.soapbridge.configuration.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.configuration.properties.SoapEndpointConfiguration;
import org.gxf.soapbridge.kafka.senders.ProxyResponseKafkaSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoapClientTest {

  @Mock ProxyResponseKafkaSender proxyResponseKafkaSender;
  @Mock HttpsUrlConnectionFactory httpsUrlConnectionFactory;
  @Mock SigningService signingService;

  private final byte[] testContent = "test content".getBytes(StandardCharsets.UTF_8);

  @Spy
  SoapConfigurationProperties soapConfigurationProperties =
      new SoapConfigurationProperties(
          HostnameVerificationStrategy.BROWSER_COMPATIBLE_HOSTNAMES,
          45,
          new HashMap<>(),
          new SoapEndpointConfiguration("localhost", 443, "https"));

  @InjectMocks SoapClient soapClient;

  @Test
  void shouldSendSoapRequestAndKafkaResponse() throws Exception {
    // arrange
    final HttpsURLConnection connection = setupConnectionMock();
    Mockito.when(
            httpsUrlConnectionFactory.createConnection(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()))
        .thenReturn(connection);

    // act
    soapClient.sendRequest("connectionId", "context", "commonName", "payload");

    // assert
    Mockito.verify(connection).disconnect();
  }

  @Test
  void shoudDisconnectWhenSoapRequestFails() throws Exception {
    // arrange
    final HttpsURLConnection connection = setupFailingConnectionMock();
    Mockito.when(
            httpsUrlConnectionFactory.createConnection(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()))
        .thenReturn(connection);

    // act
    soapClient.sendRequest("connectionId", "context", "commonName", "payload");

    // assert
    Mockito.verify(connection).disconnect();
    Mockito.verifyNoInteractions(proxyResponseKafkaSender);
  }

  private HttpsURLConnection setupConnectionMock() throws Exception {
    final HttpsURLConnection connection = Mockito.mock(HttpsURLConnection.class);
    final InputStream inputStream = new ByteArrayInputStream(testContent);
    Mockito.when(connection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
    Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    Mockito.when(connection.getInputStream()).thenReturn(inputStream);
    return connection;
  }

  private HttpsURLConnection setupFailingConnectionMock() throws Exception {
    final HttpsURLConnection connection = Mockito.mock(HttpsURLConnection.class);
    Mockito.when(connection.getOutputStream()).thenThrow(ConnectException.class);
    return connection;
  }
}
