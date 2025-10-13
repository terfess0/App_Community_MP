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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.terfess.comunidadmp"
        minSdk = 23
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.9"

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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.activity:activity-ktx:1.9.3")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.media3:media3-session:1.5.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //splash
    implementation("androidx.core:core-splashscreen:1.0.1")

    //ads
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    //firebase
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")

    //glide fotos
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //zoom fotos
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    //shimer facebook
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //swipe refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")

    //exoplayer3
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
}