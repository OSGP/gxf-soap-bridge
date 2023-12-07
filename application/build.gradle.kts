// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.gxf.utilities:kafka-azure-oauth:0.2")
    implementation("com.microsoft.azure:msal4j:1.13.10")
    implementation("org.apache.httpcomponents:httpclient:4.5.13") {
        exclude("commons-logging")
    }
    implementation(kotlin("reflect"))
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    implementation("org.springframework:spring-aspects")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")

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
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.springframework.kafka:spring-kafka-test")
                implementation("org.assertj:assertj-core")
                implementation("org.springframework.boot:spring-boot-starter-webflux")
                implementation("org.wiremock:wiremock:3.3.1")

                implementation("org.testcontainers:kafka:1.17.6")
            }
        }
    }
}
