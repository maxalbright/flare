val kotlin_version: String by extra
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization")  version "1.6.10"
    id("com.android.library")
    id("convention.publication")
}

group = "com.terathought.enchant"
version = "1.0.0-alpha10"

repositories {
    google()
    mavenCentral()
}

kotlin {
    android {
        publishAllLibraryVariants()
    }
    jvm()
    ios()
    iosSimulatorArm64()
    cocoapods {
        ios.deploymentTarget = "13.5"

        summary = "Flare"
        homepage = "https://github.com/terathought/flare"

        pod("FirebaseAnalytics", "8.10.0")
        pod("FirebaseAuth", "8.10.0")
        pod("FirebaseCore", "8.10.0")
        pod("FirebaseFirestore", "8.10.0")
        pod("FirebaseFunctions", "8.10.0")
        pod("FirebaseStorage", "8.10.0")
        pod("GoogleSignIn", "6.1.0")

        framework {
            transitiveExport = true
        }

    }


    sourceSets {

        val jvmMain by getting {
            dependencies {
                implementation("com.google.cloud:google-cloud-firestore:3.0.9")
            }
        }
        val jvmTest by getting
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

            }
        }

        val androidMain by getting {
            dependencies {
                implementation("com.google.firebase:firebase-core:20.0.2")
                implementation("com.google.firebase:firebase-firestore:24.0.0")
                implementation("com.google.firebase:firebase-auth:21.0.1")
                implementation("com.google.firebase:firebase-storage:20.0.0")
                implementation("com.google.firebase:firebase-functions:20.0.1")
                implementation("com.google.android.gms:play-services-auth:20.0.0")
                implementation("com.facebook.android:facebook-login:12.2.0")
            }
        }

        val androidAndroidTest by getting {
            dependencies {
                implementation("androidx.test:core:1.4.0")
                implementation("androidx.test.ext:junit:1.1.3")
                implementation("androidx.test:runner:1.4.0")
            }
        }

        val androidTest by getting {
            dependsOn(androidAndroidTest)
        }

        val iosMain by getting
        val iosTest by getting
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("androidTest"){
            java.srcDir(file("src/androidAndroidTest/kotlin"))
        }
    }

}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}