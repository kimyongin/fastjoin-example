plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}