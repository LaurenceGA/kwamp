import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
}


application {
    mainClassName = "io.ktor.server.netty.DevelopmentEngine"
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/ktor")
}

val ktorVersion = "0.9.5-rc13"

dependencies {
    implementation(project(":kwamp-router-core"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
}