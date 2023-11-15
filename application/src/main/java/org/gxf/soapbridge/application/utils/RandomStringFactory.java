// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.utils;

import java.security.SecureRandom;

/** This class can generate random string values. */
public class RandomStringFactory {

  /** The random used to generate random strings. */
  private static final SecureRandom random = new SecureRandom();

  /** Default length of the generated random strings. */
  private static final int DEFAULT_LENGTH = 16;

  private RandomStringFactory() {
    // Only static.
  }

  /**
   * Generate a random string.
   *
   * @return A string of length {@link RandomStringFactory#DEFAULT_LENGTH}
   */
  public static String generateRandomString() {
    return randomString(DEFAULT_LENGTH);
  }

  /**
   * Generate a random string.
   *
   * @param length The length of the random string to generate.
   * @return A string of the given length.
   */
  public static String generateRandomString(final int length) {
    return randomString(length);
  }

  private static String randomString(final int length) {
    final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    final StringBuilder buf = new StringBuilder();
    for (int i = 0; i < length; i++) {
      buf.append(chars.charAt(random.nextInt(chars.length())));
    }
    return buf.toString();
  }
}
