import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
}

group = "co.nz.arm"
version = "1.0.0"


application {
    mainClassName = "io.ktor.server.netty.DevelopmentEngine"
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/ktor")
}

val ktorVersion = "0.9.5-rc13"
val logbackVersion = "1.2.1"

dependencies {
    implementation(project(":kwamp-router"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}