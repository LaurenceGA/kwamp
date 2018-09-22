import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.3.0-rc-57"

    repositories {
        mavenCentral()
        maven(url = "http://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

group = "co.nz.arm"
version = "1.0.0"

plugins {
    base
    java
    kotlin("jvm") version "1.2.70" apply false
}

val kotlinVersion: String by extra
val kotlinTestVersion = "3.1.7"

subprojects {
    apply {
        plugin<KotlinPluginWrapper>()
    }

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "http://dl.bintray.com/kotlin/kotlin-eap")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    dependencies {
        implementation(kotlin("stdlib", kotlinVersion))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.26.1-eap13")

        testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlinTestVersion")
    }
}