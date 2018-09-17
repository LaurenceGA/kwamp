import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.61"

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
    kotlin("jvm") version "1.2.61"
}

kotlin.experimental.coroutines = Coroutines.ENABLE

val kotlinVersion: String by extra
val kotlinTestVersion = "3.1.7"

subprojects {
    apply {
        plugin<KotlinPluginWrapper>()
    }

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    dependencies {
        implementation(kotlin("stdlib", kotlinVersion))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.26.0")

        testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlinTestVersion")
    }
}