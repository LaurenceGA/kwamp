val klaxonVersion = "5.0.5"
val moshiPackVersion = "1.0.0-beta"

val apacheLang3CommonsVersion = "3.7"

dependencies {
    implementation("com.daveanthonythomas.moshipack:moshipack:$moshiPackVersion")
    implementation("com.beust:klaxon:$klaxonVersion")
    implementation("org.apache.commons:commons-lang3:$apacheLang3CommonsVersion")
}