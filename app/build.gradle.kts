plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.0"
}

android {
    namespace = "com.example.safewomen"
    compileSdk = 35
    buildFeatures {
        buildConfig = true // Enable BuildConfig generation
        viewBinding = true // Enable ViewBinding
    }
    defaultConfig {
        applicationId = "com.example.safewomen"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add your Google Maps API key as a BuildConfig field
        buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyCvOJhLdKIqB0aablZ73If3HANvUo1qRkM\"")

        // Also add it as a manifest placeholder for Maps SDK
        manifestPlaceholders["MAPS_API_KEY"] = "AIzaSyCvOJhLdKIqB0aablZ73If3HANvUo1qRkM"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17)) // ou 11 ou 8 si tu préfères
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material.v1100)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0") // Keep only the latest version

    // Google Maps Services for direct API access
    implementation("com.google.maps:google-maps-services:2.2.0")
    implementation("org.slf4j:slf4j-simple:2.0.7") // Required for Google Maps Services

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Room for local database
    implementation("androidx.room:room-runtime:2.6.0")
    implementation(libs.activity)
    implementation(libs.legacy.support.v4)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    annotationProcessor("androidx.room:room-compiler:2.6.0")

    // Navigation component
    implementation("androidx.navigation:navigation-fragment:2.7.4")
    implementation("androidx.navigation:navigation-ui:2.7.4")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.9.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
