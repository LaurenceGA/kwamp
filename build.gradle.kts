import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.51"

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

group = "co.nz.arm"
version = "1.0.0"

plugins {
    java
    application
    kotlin("jvm") version "1.2.51"
}

application {
    mainClassName = "io.ktor.server.netty.DevelopmentEngine"
}

val kotlinVersion: String by extra
val ktorVersion = "0.9.3"
val logbackVersion = "1.2.1"
val klaxonVersion = "3.0.1"

repositories {
    mavenCentral()
    jcenter()
    maven (url = "https://dl.bintray.com/kotlin/ktor")
}

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("io.ktor:ktor-websockets:${ktorVersion}")
    implementation("io.ktor:ktor-gson:${ktorVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("com.beust:klaxon:${klaxonVersion}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin.experimental.coroutines = Coroutines.ENABLE