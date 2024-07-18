import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("app.kotlin-application-conventions")
    kotlin("kapt")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    // // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.5.+"))

    // Use the Kotlin JDK
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.+")

    // Add javax bc removed in Java11 (https://bit.ly/3rcSh5P)
    implementation("javax.xml.bind:jaxb-api:2.3.+")

    // AWS lambda interface libs
    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    runtimeOnly("com.amazonaws:aws-lambda-java-log4j2:1.2.0")
//    implementation("com.amazonaws:aws-lambda-java-runtime-interface-client:2.0.0")

    // AWS sdk
    implementation(platform("software.amazon.awssdk:bom:2.17.88"))
    // implementation("software.amazon.awssdk:dynamodb")
    // implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:iot")
    // implementation("software.amazon.awssdk:iotdataplane")
    // implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:cognitoidentity")
    implementation("software.amazon.awssdk:cognitoidentityprovider")

    // AWS xray
//    implementation("com.amazonaws:aws-xray-recorder-sdk-log4j:2.10.+")
//    implementation("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2:2.2.+")
//    implementation("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2-instrumentor:2.2.+")

    // AWS iot sdk
//    implementation("software.amazon.awssdk.iotdevicesdk:aws-iot-device-sdk:1.5.4") // iot device sdk v2
    implementation("com.amazonaws:aws-iot-device-sdk-java:1.3.9") // iot sdk v1

    // log4j
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.14.+"))
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-api")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")

    // jackson
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.13.1"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // dagger
    implementation("com.google.dagger:dagger:2.42")
    kapt("com.google.dagger:dagger-compiler:2.42")

    // Use the Kotlin test library.
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.mockito:mockito-core:4.+")
    testImplementation("org.mockito:mockito-junit-jupiter:4.+")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.+")
    testImplementation("org.assertj:assertj-core:3.20.2")
}

application {
    // Define the main class for the application.
    mainClass.set("app.app.AppKt")
}

// auto lint on build
listOf("ktlintKotlinScriptCheck", "ktlintMainSourceSetCheck", "ktlintTestSourceSetCheck").forEach {
    tasks.getByName(it) {
        dependsOn("ktlintFormat")
    }
}
