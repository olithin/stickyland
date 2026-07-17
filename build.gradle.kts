import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.compose") version "1.10.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

group = "com.stickyland"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.50.1")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

compose.desktop {
    application {
        mainClass = "com.mynotes.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Stickyland"
            packageVersion = "1.0.0"
            description = "Stickyland — local notes with screenshots"
            vendor = "Stickyland"
            copyright = "© Stickyland"
            modules("java.sql")

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "Stickyland"
                upgradeUuid = "B2C3D4E5-F6A7-8901-BCDE-F12345678901"
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "com.stickyland.app"
                dockName = "Stickyland"
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
