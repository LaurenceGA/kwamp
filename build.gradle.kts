import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.3.50"

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
val kotlinTestVersion = "3.4.2"
val coroutinesVersion = "1.3.2"
val logbackVersion = "1.2.3"

subprojects {
    apply {
        plugin<KotlinPluginWrapper>()
    }

    group = "com.github.LaurenceGA"

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