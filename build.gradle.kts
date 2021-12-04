plugins {
    kotlin("multiplatform") version "1.5.31"
    id("com.android.library")
    id("maven-publish")
}

group = "com.terathought.enchant"
version = "1.0.0-alpha01"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    android()
    ios()

    sourceSets {

        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting
        val jvmTest by getting

        val jsMain by getting
        val jsTest by getting

        val androidMain by getting

        val androidAndroidTestRelease by getting

        val androidTest by getting {
            dependsOn(androidAndroidTestRelease)
        }

        val iosMain by getting
        val iosTest by getting
    }
}

android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}