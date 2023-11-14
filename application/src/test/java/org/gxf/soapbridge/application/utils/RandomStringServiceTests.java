// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.application.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RandomStringServiceTests {

  @Test
  void test1() {
    final String randomString = RandomStringFactory.generateRandomString();

    Assertions.assertNotNull(
        randomString, "It is expected that the generated random string not is null");
    Assertions.assertEquals(
        16,
        randomString.length(),
        "It is expected that the generated random string is of length 16");
  }

  @Test
  void test2() {
    final String randomString = RandomStringFactory.generateRandomString(42);

    Assertions.assertNotNull(
        randomString, "It is expected that the generated random string not is null");
    Assertions.assertEquals(
        42,
        randomString.length(),
        "It is expected that the generated random string is of length 42");
  }
}
