// Copyright 2023 Alliander N.V.

rootProject.name = "gxf-soap-bridge"

include("application")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlinLogging", "7.0.0")
            version("gxfUtils", "0.3.7")
            version("apacheHttpClient", "4.5.14")


            library("logging", "io.github.oshai", "kotlin-logging-jvm").versionRef("kotlinLogging")

            library("kafkaAzureOAuth", "com.gxf.utilities", "kafka-azure-oauth").versionRef("gxfUtils")
            library("apacheHttpClient", "org.apache.httpcomponents", "httpclient").versionRef("apacheHttpClient")
        }
        create("testLibs") {
            version("wiremock", "3.6.0")

            library("mockServer", "org.wiremock", "wiremock-standalone").versionRef("wiremock")
        }
    }
}
