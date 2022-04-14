![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=%23FF8811&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fterathought%2Fenchant%2Fflare%2Fmaven-metadata.xml)

![fire](https://oldcofh.github.io/assets/images/thermal-foundation/blaze-powder.gif)

# Flare

## Setup
Step 1: Create a Firebase Project

Link to Firebase Setup Documentation: https://firebase.google.com/docs/android/setup

Step 2: Register Your App With Firebase

Step 3: Add a Firebase Configuration File

Step 4: Add Firebase SDKs to Your App

## Get Started
You can access different services using instances:

```kotlin
val auth = FirebaseAuth.instance
val firestore = FirebaseFirestore.instance
val storage = FirebaseStorage.instance
val functions = FirebaseFunctions.instance
```

## Supported APIs
So far, there are two platforms that Flare supports: Android and iOS. Within these two platforms, Flare supports various services
that can help with Authentification, Firestore, Storage, and Cloud Functions. In the future, the hope is to add support for JavaScript
and other services.

## Cool APIs
Flare has a very functional API style. It uses Kotlin coroutines and Kotlin Serialization for FireStore,
making your backend app development experience seamless and problem free.