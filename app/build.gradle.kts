plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.james.imeetpolycc"
    compileSdk = 34

    defaultConfig {
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"ade20ce9-1dad-4f0e-b829-5f8bccd19f49\"")
        buildConfigField("String", "ONESIGNAL_REST_API_KEY", "\"NzdiMzg1N2QtM2FlZi00YWY3LTgzZjAtMmMxNTE3YTk2Mzcy\"")

        applicationId = "com.james.imeetpolycc"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.firebase:firebase-firestore:25.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.0")

    // Glide dependencies
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // OneSignal dependencies
    implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")

    // UCrop dependency
    implementation("com.github.Yalantis:ucrop:2.2.8")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
}