plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.akash.leakmasterv2"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.akash.leakmasterv2"
        minSdk = 24
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))

    // Add the dependency for the Realtime Database library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.squareup.okhttp3:okhttp:4.9.1") // OkHttp dependency
    implementation("com.airbnb.android:lottie:6.1.0") // Lottie Files dependency
    implementation("com.github.AtifSayings:Animatoo:1.0.1") // Animatoo library
    implementation("com.github.yagmurerdogan:Toastic:1.0.1") // Custom Toast library
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Retrofit2 library
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Retrofit2 to GSON Converter library
}