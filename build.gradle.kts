plugins {
    kotlin("multiplatform") version "1.6.0"
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

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1-new-mm-dev2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0-RC")

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

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
}