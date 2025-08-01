// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(libs.springBootStarterActuator)
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterWebflux)
    implementation(libs.springBootStarterSecurity)
    implementation(libs.springBootStarterLogging)
    implementation(libs.springKafka)
    implementation(libs.kafkaAzureOAuth)
    implementation(libs.apacheHttpClient) {
        exclude("commons-logging")
    }
    implementation(kotlin("reflect"))
    implementation(libs.logging)

    implementation("org.springframework:spring-aspects")

    runtimeOnly(libs.micrometerPrometheusModule)
    runtimeOnly(libs.springBootDependencies)
    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.mockitoJunitJupiter)
    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitPlatformLauncher)

    // Generate test and integration test reports
    jacocoAggregation(project(":application"))
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
    imageName.set("ghcr.io/osgp/gxf-soap-bridge:${version}")
    if (project.hasProperty("publishImage")) {
        publish.set(true)
        docker {
            publishRegistry {
                username.set(System.getenv("GITHUB_ACTOR"))
                password.set(System.getenv("GITHUB_TOKEN"))
            }
        }
    }
}

testing {
    suites {
        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(libs.springBootStarterTest)
                implementation(libs.springKafkaTest)
                implementation(libs.assertJ)
                implementation(libs.springBootStarterWebflux)
                implementation(libs.mockServer)
            }
        }
    }
}
