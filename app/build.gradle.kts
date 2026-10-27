import java.util.Properties

// Read secrets from local.properties (this file is not committed to Git)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")   // reads google-services.json
}

android {
    namespace = "com.example.geoguessindia"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.geoguessindia"
        minSdk = 24
        targetSdk = 34          // your brief says 34; raise it before publishing
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Becomes BuildConfig.STADIA_API_KEY in Kotlin
        buildConfigField(
            "String",
            "STADIA_API_KEY",
            "\"${localProps.getProperty("STADIA_API_KEY", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Map: osmdroid draws the Stadia Maps tiles
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Firebase: the BoM keeps all Firebase library versions in sync
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}