plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.recipevault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.recipevault"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    // Update Firebase BOM to latest version
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

    // Firebase services - remove -ktx if you're using Java
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase UI (update version)
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    // Image loading for recipe photos
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // For RecyclerView (needed for recipe lists)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // For future networking (ZaloPay integration)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Fragment and Navigation components
    implementation("androidx.fragment:fragment:1.6.2")
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    implementation("androidx.activity:activity:1.8.2")
}