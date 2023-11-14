// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.services;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.HexFormat;
import org.gxf.soapbridge.configuration.properties.SecurityConfigurationProperties;
import org.gxf.soapbridge.configuration.properties.SigningConfigurationProperties;
import org.gxf.soapbridge.soap.exceptions.ProxyServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This {@link @Service} class can generate a signature for a given content, and verify the content
 * using the signature.
 */
@Service
public class SigningService {
  private final SigningConfigurationProperties signingConfiguration;

  private static final Logger LOGGER = LoggerFactory.getLogger(SigningService.class);

  private SigningService(final SecurityConfigurationProperties securityConfiguration) {
    signingConfiguration = securityConfiguration.getSigning();
  }

  /**
   * Using the given content, create a signature. The signature is converted from byte array to
   * hexadecimal string.
   *
   * @param content The content to sign.
   * @return The signature which can be used later to verify if the content is intact and unaltered.
   * @throws ProxyServerException thrown when an error occurs during signing.
   */
  public String signContent(final String content) throws ProxyServerException {
    final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    try {
      final byte[] signature = createSignature(bytes);
      LOGGER.debug("signature.length: {}", signature.length);
      return HexFormat.of().formatHex(signature);
    } catch (final GeneralSecurityException e) {
      throw new ProxyServerException(
          "Unexpected GeneralSecurityException when trying to sign the content", e);
    }
  }

  /**
   * For the given content and security key, verify if the content is intact and unaltered.
   *
   * @param content The content to verify.
   * @param securityKey The signature a.k.a. security key which was created using {@link
   *     SigningService#signContent(String)}.
   * @return True when the verification succeeds.
   */
  public boolean verifyContent(final String content, final String securityKey) {
    final byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    final byte[] securityKeyBytes = HexFormat.of().parseHex(securityKey);
    LOGGER.debug("securityKeyBytes.length: {}", securityKeyBytes.length);
    try {
      return validateSignature(contentBytes, securityKeyBytes);
    } catch (final GeneralSecurityException e) {
      LOGGER.error(
          "Unexpected GeneralSecurityException when trying to verify the content using the security key",
          e);
      return false;
    }
  }

  private byte[] createSignature(final byte[] message) throws GeneralSecurityException {
    final Signature signatureBuilder =
        Signature.getInstance(
            signingConfiguration.getSignature(), signingConfiguration.getProvider());
    signatureBuilder.initSign(signingConfiguration.getSignKey());
    signatureBuilder.update(message);
    return signatureBuilder.sign();
  }

  private boolean validateSignature(final byte[] message, final byte[] securityKey)
      throws GeneralSecurityException {
    final Signature signatureBuilder =
        Signature.getInstance(
            signingConfiguration.getSignature(), signingConfiguration.getProvider());
    signatureBuilder.initVerify(signingConfiguration.getVerifyKey());
    signatureBuilder.update(message);
    return signatureBuilder.verify(securityKey);
  }
}
