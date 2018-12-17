import groovy.lang.Closure
import org.gradle.internal.impldep.org.apache.ivy.core.module.descriptor.Artifact
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.3.10"

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

plugins {
    base
    java
    `maven-publish`
}

val kotlinVersion: String by extra
val kotlinTestVersion = "3.1.10"
val coroutinesVersion = "1.0.1"
val logbackVersion = "1.2.1"

subprojects {
    apply {
        plugin<KotlinPluginWrapper>()
    }

    group = "com.github.LaurenceGA"

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
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
        implementation(kotlin("reflect", kotlinVersion))
        implementation("ch.qos.logback:logback-classic:$logbackVersion")

        testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlinTestVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("router") {
            val project = project(":kwamp-router-core")
            groupId = project.group as String
            artifactId = project.properties["artifact_id"] as String

            from(project.components["java"])

            pom {
                name.set("Router")
            }
        }
    }
    publications {
        create<MavenPublication>("client") {
            val project = project(":kwamp-client-core")
            groupId = project.group as String
            artifactId = project.properties["artifact_id"] as String

            from(project.components["java"])

            pom {
                name.set("Client")
            }
        }
    }
    publications {
        create<MavenPublication>("core") {
            val project = project(":kwamp-core")
            groupId = project.group as String
            artifactId = project.properties["artifact_id"] as String

            from(project.components["java"])

            pom {
                name.set("Core")
            }
        }
    }
}