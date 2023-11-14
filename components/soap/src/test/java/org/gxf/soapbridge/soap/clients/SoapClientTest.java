// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.soap.clients;

import org.gxf.soapbridge.application.factories.HttpsUrlConnectionFactory;
import org.gxf.soapbridge.application.properties.HostnameVerificationStrategy;
import org.gxf.soapbridge.application.properties.SoapConfigurationProperties;
import org.gxf.soapbridge.application.properties.SoapEndpointConfiguration;
import org.gxf.soapbridge.application.services.SigningService;
import org.gxf.soapbridge.messaging.ProxyResponsesMessageSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoapClientTest {

    @Mock
    ProxyResponsesMessageSender proxyResponsesMessageSender;
    @Mock
    HttpsUrlConnectionFactory httpsUrlConnectionFactory;
    @Mock
    SigningService signingService;

    byte[] testContent = "test content".getBytes(StandardCharsets.UTF_8);

    @Spy
    SoapConfigurationProperties soapConfigurationProperties = new SoapConfigurationProperties(
            HostnameVerificationStrategy.BROWSER_COMPATIBLE_HOSTNAMES,
            45,
            "",
            new SoapEndpointConfiguration(
                    "localhost", 443, "https"
            )
    );

    @InjectMocks
    SoapClient soapClient;

    @Test
    void shouldSendSoapRequestAndJmsResponse() throws Exception {
        // arrange
        final HttpsURLConnection connection = setupConnectionMock();
        when(httpsUrlConnectionFactory.createConnection(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(connection);

        // act
        soapClient.sendRequest(
                "connectionId",
                "context",
                "commonName",
                "payload");

        // assert
        verify(connection).disconnect();
    }

    @Test
    void shoudDisconnectWhenSoapRequestFails() throws Exception {
        // arrange
        final HttpsURLConnection connection = setupFailingConnectionMock();
        when(httpsUrlConnectionFactory.createConnection(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(connection);

        // act
        soapClient.sendRequest(
                "connectionId",
                "context",
                "commonName",
                "payload");

        // assert
        verify(connection).disconnect();
        verifyNoInteractions(proxyResponsesMessageSender);
    }

    private HttpsURLConnection setupConnectionMock() throws Exception {
        final HttpsURLConnection connection = mock(HttpsURLConnection.class);
        final InputStream inputStream = new ByteArrayInputStream(testContent);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(inputStream);
        return connection;
    }

    private HttpsURLConnection setupFailingConnectionMock() throws Exception {
        final HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(connection.getOutputStream()).thenThrow(ConnectException.class);
        return connection;
    }
}
