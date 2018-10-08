import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nz.co.arm"
version = "1.0.0"

val klaxonVersion = "3.0.6"
val moshiPackVersion = "1.0.0-beta"

val apacheLang3CommonsVersion = "3.7"

dependencies {
    implementation("com.daveanthonythomas.moshipack:moshipack:$moshiPackVersion")
    implementation("com.beust:klaxon:$klaxonVersion")
    implementation("org.apache.commons:commons-lang3:$apacheLang3CommonsVersion")
}