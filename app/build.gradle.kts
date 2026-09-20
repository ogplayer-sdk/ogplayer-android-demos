plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ogplayer.demos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ogplayer.demos"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required by Media3 1.8+ / bundled IMA in any app consuming OGPlayer.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            // Deliberately NO app-side keep rules for the SDK: this build
            // verifies that the consumer proguard rules shipped inside the
            // AARs are sufficient on their own (the customer experience).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    // OGPlayer SDK from Maven Central
    implementation("tv.ogplayer:ogplayer-core:1.4.0")
    implementation("tv.ogplayer:ogplayer-ui:1.4.0")
    implementation("tv.ogplayer:ogplayer-ads-ima:1.4.0")
    implementation("tv.ogplayer:ogplayer-cast:1.4.0")
    implementation("tv.ogplayer:ogplayer-ads-freewheel:1.4.0")
    // implementation(files("libs/FWAdManager.aar")) // your licensed FreeWheel SDK

    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.core:core-ktx:1.15.0")

    // Cast button (media route picker dialog needs AppCompat)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")
}
