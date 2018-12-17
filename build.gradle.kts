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
//    maven
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

//    group = "com.laurencegarmstrong.kwamp"
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

task("artifacts") {
    doLast {
        val allArtifacts = configurations.archives.allArtifacts
        println("Project has ${allArtifacts.size} artifacts:")
        allArtifacts.forEach { artifact ->
            println("'${artifact.name}'")
        }
    }
}

val kwampRouter = project(":kwamp-router-core").tasks["jar"]
val kwampClient = project(":kwamp-client-core").tasks["jar"]

publishing {
    publications {
        create<MavenPublication>("router") {
            val project = project(":kwamp-router-core")
            groupId = project.group as String
            artifactId = project.properties["artifact_id"] as String
            version = project.version as String

            from(project.components["java"])
        }
    }
    publications {
        create<MavenPublication>("client") {
            val project = project(":kwamp-client-core")
            groupId = project.group as String
            artifactId = project.properties["artifact_id"] as String
            version = project.version as String

            from(project.components["java"])
        }
    }
}

//artifacts {
//    add("archives", kwampRouter) {
//        type = "jar"
//        name = "kwamp-router-core"
//        group = project("kwamp-router-core").group
//        version = project("kwamp-router-core").version
//        classifier = project("kwamp-router-core").properties["classifier"] as String
//        builtBy(kwampRouter)
//    }
//    add("archives", kwampClient) {
//        type = "jar"
//        name = "kwamp-client-core"
//        group = project("kwamp-client-core").group
//        version = project("kwamp-client-core").version
//        classifier = project("kwamp-client-core").properties["classifier"] as String
//        builtBy(kwampClient)
//    }
//}
//
//// We don't want the artifact produced by the root project
//configurations.archives.allArtifacts.removeIf { artifact ->
//    artifact.name == "kwamp"
//}

//val filter = object : PublishFilter {
//    override fun accept(p0: Artifact?, p1: File?): Boolean {
//        return true
//    }
//}

//tasks.getByName<Upload>("install") {
//    repositories.withGroovyBuilder {
//        "mavenInstaller" {
//            "addFilter"("test", filter)
//        }
//    }
//    val installer = repositories["mavenInstaller"] as MavenResolver
//    installer.addFilter("router") { artifact, _ ->
//        artifact.name == "kwamp-router-core"
//    }

//    installer.addFilter("client") { artifact, _ ->
//        artifact.name == "kwamp-client-core"
//    }
//    repositories.withGroovyBuilder {
//        val installer = getProperty("mavenInstaller") as MavenResolver
//        installer.addFilter("router") { artifact, _ ->
//            artifact.name == "kwamp-router-core"
//        }
//        installer.addFilter("client") { artifact, _ ->
//            artifact.name == "kwamp-client-core"
//        }
//    }
//}

//install {
//    repositories.mavenInstaller {
//        addFilter('api') { artifact, file -> artifact.name.endsWith('api') }
//        addFilter('impl') { artifact, file -> artifact.name.endsWith('impl') }
//    }
//}

//tasks.getByName<Upload>("uploadArchives") {
//    repositories.withGroovyBuilder {
//        "mavenDeployer" {
//            "repository"("url" to "file://localhost/tmp/myRepo/")
//        }
//    }
//}