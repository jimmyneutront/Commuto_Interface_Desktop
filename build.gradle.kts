import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("org.jetbrains.compose") version "0.4.0"
    id("com.squareup.sqldelight") version "1.5.1"
}

group = "me.jimmyt"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven( "https://www.jetbrains.com/intellij-repository/releases")
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.1")
    implementation("org.web3j:core:4.8.7")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    //implementation("org.bouncycastle:bcpg-jdk15on:1.69")
    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Commuto_Interface_Desktop"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    database("CommutoInterfaceDB") {
        packageName = "com.commuto.interfacedesktop.db"
    }
}