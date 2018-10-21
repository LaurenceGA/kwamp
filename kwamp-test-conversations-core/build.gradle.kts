import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinTestVersion = "3.1.10"

dependencies {
    compile(project(":kwamp-router-core"))
    compile(project(":kwamp-client-core"))

    implementation("io.kotlintest:kotlintest-assertions:$kotlinTestVersion")
}