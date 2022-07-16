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

// Trixnity Matrix SDK
val trixnityVersion = "2.1.1"
fun trixnity(module: String, version: String = trixnityVersion) =
    "net.folivo:trixnity-$module:$version"

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

configurations {
    // A configuration meant for consumers that need the implementation of this component
    create("exposedRuntime") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }
}

dependencies {
    // Compose for Desktop
    implementation(compose.desktop.currentOs)
    // Dagger for dependency injection
    implementation("com.google.dagger:dagger:2.42")
    kapt("com.google.dagger:dagger-compiler:2.42")
    // SQLDelight for database operations
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.3")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3") {
        because("SQLDelight depends on this, but we need it in the compile classpath so we can catch " +
                "exceptions defined in it")
    }
    // BouncyCastle for cryptography
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

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

    // Web3J for blockchain-related operations
    //TODO: Update these to a version with no vulnerabilities
    implementation("org.web3j:codegen:4.8.7")
    implementation("org.web3j:contracts:4.8.7")
    implementation("org.web3j:core:4.8.7")
    //implementation("org.bouncycastle:bcpg-jdk15on:1.69")
    testImplementation(kotlin("test"))
    // Trixnity Matrix SDK
    implementation(trixnity("clientserverapi-client"))
    // Ktor engine for Trixnity
    implementation("io.ktor:ktor-client-okhttp:2.0.2")
    // Serialization Library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    // Log4j 2 for logging
    implementation("org.apache.logging.log4j:log4j-api:2.18.0")
    implementation("org.apache.logging.log4j:log4j-core:2.18.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")
    // Ktor Content negotiation plugin for interactions with TestingServer
    testImplementation("io.ktor:ktor-client-content-negotiation:2.0.2")
    // Ktor JSON serialization plugin for interactions with TestingServer
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:2.0.2")
    // Kotlin Coroutines testing utilities
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.6.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs = kotlinOptions.freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
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
        packageName = "com.commuto.interfacedesktop.database"
    }
}