// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
rootProject.name = "gxf-soap-bridge"

include("application")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlinLogging", "7.0.0")
            version("gxfUtils", "2.1")
            version("apacheHttpClient", "5.4")


            library("logging", "io.github.oshai", "kotlin-logging-jvm").versionRef("kotlinLogging")

            library("kafkaAzureOAuth", "com.gxf.utilities", "kafka-azure-oauth").versionRef("gxfUtils")
            library("apacheHttpClient", "org.apache.httpcomponents.client5", "httpclient5").versionRef("apacheHttpClient")
        }
        create("testLibs") {
            version("wiremock", "3.9.2")

            library("mockServer", "org.wiremock", "wiremock-standalone").versionRef("wiremock")
        }
    }
}
