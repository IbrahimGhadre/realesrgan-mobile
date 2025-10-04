plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.realesrganappncnn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.realesrganappncnn"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Imposta ABI solo su arm64-v8a per evitare errori
        ndk {
            abiFilters.add("arm64-v8a")
        }
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
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }

    // Assicura che libc++_shared.so sia incluso nella build
    packagingOptions {
        pickFirst("lib/arm64-v8a/libc++_shared.so")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}