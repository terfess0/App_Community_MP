plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    //google services for firebase
    id("com.google.gms.google-services")
    //safeargs
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.terfess.comunidadmp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.terfess.comunidadmp"
        minSdk = 23
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("com.google.ar.sceneform:filament-android:1.17.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //splash
    implementation("androidx.core:core-splashscreen:1.0.1")

    //ads
    implementation("com.google.android.gms:play-services-ads:23.2.0")

    //firebase
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")

    //glide fotos
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //zoom fotos
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    //shimer facebook
    implementation("com.facebook.shimmer:shimmer:0.5.0")
}