plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services") // Firebase용
    id("kotlin-kapt")
}

android {
    namespace = "com.example.dogwalkapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dogwalkapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["kakaoScheme"] = "kakaoc6652648e6d66eb28c5a75fec2e683cb"  // 카카오 네이티브 앱 키

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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.2.2")) //파이어베이스
    implementation("com.google.firebase:firebase-analytics-ktx") //파이어베이스


    implementation("com.kakao.sdk:v2-user:2.19.0") // 카카오 로그인

    implementation ("com.google.android.gms:play-services-maps:18.1.0")       // 지도
    implementation ("com.google.android.gms:play-services-location:21.0.1")  // 위치

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}