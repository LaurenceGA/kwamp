import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nz.co.arm"
version = "1.0.0"

val logbackVersion = "1.2.1"

dependencies {
    compile(project(":kwamp-core"))

    //TODO move logging to root project dependency
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}