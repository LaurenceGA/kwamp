import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.51"

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlin_version))
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

val kotlin_version: String by extra
val ktor_version = "0.9.3"
val logback_version = "1.2.1"

repositories {
    mavenCentral()
    jcenter()
    maven (url = "https://dl.bintray.com/kotlin/ktor")
}

dependencies {
    compile(kotlin("stdlib", kotlin_version))
    compile("io.ktor:ktor-server-core:${ktor_version}")
    compile("io.ktor:ktor-server-netty:${ktor_version}")
    compile("io.ktor:ktor-websockets:${ktor_version}")
    compile("io.ktor:ktor-gson:${ktor_version}")
    compile("ch.qos.logback:logback-classic:${logback_version}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin.experimental.coroutines = Coroutines.ENABLE