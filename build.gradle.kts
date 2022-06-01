import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    //Required for Dagger
    kotlin("kapt") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.compose") version "1.0.1"
    id("com.squareup.sqldelight") version "1.5.1"
}

kapt {
    generateStubs = true
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
    implementation("com.google.dagger:dagger:2.42")
    kapt("com.google.dagger:dagger-compiler:2.42")
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.1")
    implementation("io.ktor:ktor-client-java:1.6.7")
    implementation("net.folivo:trixnity-client-api:1.0.0-RC3")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")

    // For using local web3j build
    /*
    implementation(files("web3j/abi/build/libs/abi-4.8.9-SNAPSHOT.jar"))
    implementation(files("web3j/codegen/build/libs/codegen-4.8.9-SNAPSHOT.jar"))
    implementation(files("web3j/contracts/build/libs/contracts-4.8.9-SNAPSHOT.jar"))
    implementation(files("web3j/core/build/libs/core-4.8.9-SNAPSHOT.jar"))
    implementation(files("web3j/crypto/build/libs/crypto-4.8.9-SNAPSHOT.jar"))
    implementation(files("web3j/rlp/build/libs/rlp-4.8.9-SNAPSHOT.jar"))
    implementation(files("web3j/utils/build/libs/utils-4.8.9-SNAPSHOT.jar"))

    implementation("io.reactivex.rxjava2:rxjava:2.2.2")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
    */

    implementation("org.web3j:codegen:4.8.7")
    implementation("org.web3j:contracts:4.8.7")
    implementation("org.web3j:core:4.8.7")
    //implementation("org.bouncycastle:bcpg-jdk15on:1.69")
    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
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