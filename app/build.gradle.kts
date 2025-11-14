plugins {
    alias(libs.plugins.android.application)
    // Add the Google service Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.carboncalculator"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.carboncalculator"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // 1) Firebase Auth (for login/register)
    implementation("com.google.firebase:firebase-auth")

    // 2) Firestore
    implementation("com.google.firebase:firebase-firestore")

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}