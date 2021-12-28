plugins {
    kotlin("multiplatform") version "1.6.10"
    kotlin("native.cocoapods") version "1.6.10"
    id("com.android.library")
    id("maven-publish")
}

group = "com.terathought.enchant"
version = "1.0.0-alpha01"

repositories {
    google()
    mavenCentral()
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion =
        "16.0.0"
}

kotlin {
    jvm()
    js {
        browser {
            testTask {
                useMocha()
            }
        }
    }
    android()
    ios()
    iosSimulatorArm64()
    cocoapods {
        ios.deploymentTarget = "13.5"

        summary = "CocoaPods test library"
        homepage = "https://github.com/JetBrains/kotlin"

        pod("FirebaseAnalytics", "8.10.0")
        pod("FirebaseAuth", "8.10.0")
        pod("FirebaseCore", "8.10.0")
        pod("FirebaseFirestore", "8.10.0")
        pod("FirebaseFunctions", "8.10.0")
        pod("FirebaseStorage", "8.10.0")
    }


    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.google.firebase:firebase-admin:8.1.0")
            }
        }
        val jvmTest by getting

        val jsMain by getting {
            dependencies {
                implementation(npm("firebase", "9.6.1"))
            }
        }
        val jsTest by getting

        val androidMain by getting {
            dependencies {
                implementation("com.google.firebase:firebase-core:20.0.2")
                implementation("com.google.firebase:firebase-firestore:24.0.0")
                implementation("com.google.firebase:firebase-auth:21.0.1")
                implementation("com.google.firebase:firebase-storage:20.0.0")
                implementation("com.google.firebase:firebase-functions:20.0.1")
            }
        }

        val androidAndroidTestRelease by getting

        val androidTest by getting {
            dependsOn(androidAndroidTestRelease)
        }

        val iosMain by getting
        val iosTest by getting

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }
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