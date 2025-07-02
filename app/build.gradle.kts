plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs")
}

android {
    namespace = "com.example.cooking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cooking"
        minSdk = 23
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Путь к файлу keystore
            storeFile = rootDir.resolve("app/keystore/cooking-release.jks")
            // Значения берём из gradle.properties (или системных переменных)
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: ""
            keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") ?: project.findProperty("RELEASE_STORE_PASSWORD")) as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    // Добавляем поддержку загрузки шрифтов из Google Fonts
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.swiperefreshlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("io.socket:socket.io-client:2.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.core:core:1.12.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-reactivestreams:2.6.2")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("com.squareup.picasso:picasso:2.8")

    // WorkManager для фоновой синхронизации 
    implementation("androidx.work:work-runtime:2.9.0")

    // RxJava
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")

    // Зависимости Navigation Component
    val nav_version = "2.7.7"

    implementation("androidx.navigation:navigation-fragment:$nav_version") // Java версия
    implementation("androidx.navigation:navigation-ui:$nav_version") // Java версия
}