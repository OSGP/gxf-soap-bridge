// Copyright 2023 Alliander N.V.
package org.gxf.soapbridge.application.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RandomStringServiceTests {

    @Test
    void test1() {
        final String randomString = RandomStringFactory.generateRandomString();

        assertNotNull(randomString, "It is expected that the generated random string not is null");
        assertEquals(
                16,
                randomString.length(),
                "It is expected that the generated random string is of length 16");
    }

    @Test
    void test2() {
        final String randomString = RandomStringFactory.generateRandomString(42);

        assertNotNull(randomString, "It is expected that the generated random string not is null");
        assertEquals(
                42,
                randomString.length(),
                "It is expected that the generated random string is of length 42");
    }
}
